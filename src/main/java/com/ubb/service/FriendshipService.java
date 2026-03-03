package com.ubb.service;

import com.ubb.domain.Friendship;
import com.ubb.domain.Observer;
import com.ubb.domain.Tuple;
import com.ubb.domain.User;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.FriendshipRepositoryInterface;
import com.ubb.repository.Repository;
import com.ubb.service.exceptions.EntityNotFoundException;
import com.ubb.service.exceptions.FriendshipAlreadyExistsException;
import com.ubb.service.exceptions.ServiceException;
import com.ubb.utils.dto.FriendshipDTO;
import com.ubb.utils.paging.Page;
import com.ubb.utils.paging.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serviciu care gestioneaza logica de business pentru entitatile Friendship.
 * Se asigura de validarea, unicitatea si persistenta prieteniilor.
 */
public class FriendshipService implements FriendshipServiceInterface {

    // Dependentele (injectate prin constructor)
    private final FriendshipRepositoryInterface friendshipRepository;
    private final UserServiceInterface userService;
    private final Repository<Long, User> userRepository; // Folosit pentru verificarea existentei userilor
    private final Validator<Friendship> friendshipValidator;

    private final List<Observer<Friendship>> observers = new ArrayList<>();
    /**
     * Constructor cu Dependency Injection.
     */
    public FriendshipService(
            FriendshipRepositoryInterface friendshipRepository, UserServiceInterface userService,
            Repository<Long, User> userRepository,
            Validator<Friendship> friendshipValidator) {

        this.friendshipRepository = friendshipRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.friendshipValidator = friendshipValidator;
    }

    @Override
    public void addObserver(Observer<Friendship> observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer<Friendship> observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Friendship eventData) {
        // Notificam observerii (HomeController, etc.)
        observers.forEach(observer -> observer.update(this, eventData));
    }

    // --- METODE CRUD ---

    /**
     * Creeaza si adauga o noua prietenie.
     * Include logica de verificare a existentei userilor si a unicitatii prieteniei.
     */
    @Override
    public void addFriendship(Long idUser1, Long idUser2) {
        if (idUser1 == null || idUser2 == null) {
            throw new IllegalArgumentException("ID-urile nu pot fi nule.");
        }

        // 1. Logica de Business: Verifica daca utilizatorii exista
        if (userRepository.findOne(idUser1).isEmpty()) {
            throw new EntityNotFoundException("Utilizatorul cu ID-ul " + idUser1 + " nu exista.");
        }
        if (userRepository.findOne(idUser2).isEmpty()) {
            throw new EntityNotFoundException("Utilizatorul cu ID-ul " + idUser2 + " nu exista.");
        }

        // 2. Creeaza entitatea (Constructorul Friendship standardizeaza Tuple-ul intern)
        Friendship friendship = new Friendship(idUser1, idUser2);

        // 3. Validare (Verifica regulile de domeniu, ex: ID-uri diferite)
        friendshipValidator.validate(friendship);

        // 4. Logica de Business: Verifica daca prietenia exista deja (folosind ID-ul standardizat)
        if (friendshipRepository.findOne(friendship.getId()).isPresent()) {
            throw new FriendshipAlreadyExistsException("Prietenia intre " + idUser1 + " si " + idUser2 + " exista deja.");
        }

        // 5. Persistenta
        friendshipRepository.save(friendship);
        notifyObservers(friendship);
    }

    /**
     * Sterge o prietenie intre doi utilizatori, indiferent de ordinea ID-urilor.
     */
    @Override
    public void deleteFriendship(Long idUser1, Long idUser2) {
        if (idUser1 == null || idUser2 == null) {
            throw new IllegalArgumentException("ID-urile nu pot fi nule.");
        }

        // ID-ul de sters este Tuple-ul standardizat (ID mic, ID mare), creat de constructorul Tuple
        Tuple<Long, Long> id = new Tuple<>(idUser1, idUser2);

        Optional<Friendship> deleted = friendshipRepository.delete(id);

        if (deleted.isEmpty()) {
            throw new EntityNotFoundException("Prietenia intre " + idUser1 + " si " + idUser2 + " nu a fost gasita.");
        }
        notifyObservers(deleted.get());
    }

    /**
     * Gaseste o prietenie dupa ID-urile celor doi utilizatori.
     */
    @Override
    public Optional<Friendship> findFriendship(Long idUser1, Long idUser2) {
        if (idUser1 == null || idUser2 == null) {
            throw new IllegalArgumentException("ID-urile nu pot fi nule.");
        }
        // Construim Tuple-ul pentru cautare (Tuple se standardizeaza intern)
        Tuple<Long, Long> id = new Tuple<>(idUser1, idUser2);
        return friendshipRepository.findOne(id);
    }

    /**
     * Returneaza toate prieteniile din sistem.
     */
    @Override
    public Iterable<Friendship> findAllFriendships() {
        return friendshipRepository.findAll();
    }

    // --- METODE DE PAGINARE (cu Stream-uri pentru DTO) ---

    /**
     * Obtine o pagina de prietenii si le converteste in DTO-uri care includ Username-urile.
     */
    @Override
    public Page<FriendshipDTO> findAllPaginated(Pageable pageable) {

        // 1. Obtine metadatele paginarii de la Repository (Count)
        long totalElements = friendshipRepository.count();
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        // Daca pagina este in afara limitelor, returnam o pagina goala (logica de business)
        if (pageable.getPageNumber() > totalPages && totalElements > 0) {
            return new Page<>(List.of(), pageable.getPageNumber(), totalPages, totalElements);
        }

        // 2. Obtine pagina de Friendship-uri de la Repository (Content)
        List<Friendship> friendships = friendshipRepository.findAll(pageable); // Lista de Friendships originale

        // 3. Converteste lista Friendship in lista FriendshipDTO (OPERATIE PE STREAM-URI)
        List<FriendshipDTO> dtoList = friendships.stream()
                .map(friendship -> {
                    // Cauta Username-urile (Delegare catre UserService)
                    String user1 = userService.findUser(friendship.getIdUser1())
                            // Daca userul nu exista (e.g. sters), folosim ID-ul ca placeholder
                            .map(User::getUsername).orElse(friendship.getIdUser1().toString());
                    String user2 = userService.findUser(friendship.getIdUser2())
                            .map(User::getUsername).orElse(friendship.getIdUser2().toString());

                    return new FriendshipDTO(
                            friendship.getIdUser1(), user1,
                            friendship.getIdUser2(), user2,
                            friendship.getFriendsFrom()
                    );
                })
                .collect(Collectors.toList());

        // 4. Returneaza obiectul Page cu DTO-urile
        return new Page<>(dtoList, pageable.getPageNumber(), totalPages, totalElements);
    }

    // --- Implementarea bazata pe obiectul User ---
    @Override
    public List<User> getFriendsOfUser(User user) throws ServiceException {
        if (user == null || user.getId() == null) {
            throw new ServiceException("Utilizatorul furnizat este invalid.");
        }
        // Delegeaza catre metoda bazata pe ID
        return getFriendsOfUser(user.getId());
    }

    // --- Implementarea bazata pe Long userId (Logica principala) ---
    @Override
    public List<User> getFriendsOfUser(Long userId) throws ServiceException {
        // 1. Validare Existenta User (optional, dar recomandat)
        try {
            userService.findUser(userId).orElseThrow(
                    () -> new EntityNotFoundException("Utilizatorul cu ID " + userId + " nu exista.")
            );
        } catch (EntityNotFoundException e) {
            throw new ServiceException(e.getMessage(), e);
        }

        // 2. Extragerea ID-urilor din Repository
        List<Long> friendIds = friendshipRepository.findFriendsOf(userId);

        // 3. Hidratarea (Transformarea ID-urilor in obiecte User)
        List<User> friends = new ArrayList<>();

        for (Long friendId : friendIds) {
            // Folosim userService pentru a extrage fiecare obiect User
            try {
                userService.findUser(friendId).ifPresent(friends::add);
            } catch (ServiceException e) {
                // Gestionam cazul in care un User exista in tabela de friendships dar nu in tabela users
                System.err.println("Avertisment: Utilizatorul cu ID " + friendId + " nu a putut fi gasit in User Service.");
            }
        }

        return friends;
    }

    @Override
    public List<FriendshipDTO> getFriendshipDTOs(Long userId) throws ServiceException {
        // 1. Validare Existenta User
        userService.findUser(userId).orElseThrow(() -> new EntityNotFoundException("Utilizatorul nu exista."));

        // 2. Extrage toate obiectele Friendship (ID-uri si Data)
        // Presupunem ca Repository-ul are o metoda pentru a extrage obiecte Friendship complete
        List<Friendship> friendships = friendshipRepository.findFriendshipsOf(userId);

        // 3. Converteste in FriendshipDTO-uri
        return friendships.stream()
                .map(f -> {
                    Long id1 = f.getId().getFirst();
                    Long id2 = f.getId().getSecond();

                    // Caută obiectele User pentru a obține username-urile
                    User user1 = userService.findUser(id1).orElse(null);
                    User user2 = userService.findUser(id2).orElse(null);

                    if (user1 != null && user2 != null) {
                        // Creează DTO-ul complet
                        return new FriendshipDTO(
                                id1, user1.getUsername(),
                                id2, user2.getUsername(),
                                f.getFriendsFrom()
                        );
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    @Override
    public Page<FriendshipDTO> findAllPaginatedForUser(Pageable pageable, Long userId) {
        if (userId == null) throw new IllegalArgumentException("ID utilizator invalid.");
        return friendshipRepository.findAllPaginatedForUser(pageable, userId);
    }

    @Override
    public boolean areFriends(Long id1, Long id2) {
        // Verificăm ambele variante ale tuplului în repository
        return friendshipRepository.findOne(new Tuple<>(id1, id2)).isPresent() ||
                friendshipRepository.findOne(new Tuple<>(id2, id1)).isPresent();
    }

    @Override
    public List<User> getAllFriends(Long userId) {
        List<User> friends = new ArrayList<>();

        // Obținem toate prieteniile din repository
        friendshipRepository.findAll().forEach(f -> {
            if (f.getId().getFirst().equals(userId)) {
                // Dacă userId este în stânga, prietenul este cel din dreapta
                userService.findUser(f.getId().getSecond()).ifPresent(friends::add);
            } else if (f.getId().getSecond().equals(userId)) {
                // Dacă userId este în dreapta, prietenul este cel din stânga
                userService.findUser(f.getId().getFirst()).ifPresent(friends::add);
            }
        });

        return friends;
    }
}