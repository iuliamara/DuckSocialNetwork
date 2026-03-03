package com.ubb.service;

import com.ubb.domain.*;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.UserRepositoryInterface;
import com.ubb.service.exceptions.EntityNotFoundException;
import com.ubb.utils.paging.Page;
import com.ubb.utils.paging.Pageable;
import com.ubb.utils.PasswordHasher;
import com.ubb.service.exceptions.AuthenticationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serviciu care gestioneaza logica de business pentru entitatile User.
 * Include operatii CRUD, validare polimorfica, autentificare si paginare/filtrare.
 */
public class UserService implements UserServiceInterface {

    // Dependentele (injectate prin constructor)
    private final UserRepositoryInterface userRepository;
    private final Validator<Persoana> persoanaValidator;
    private final Validator<SwimmingDuck> swimmingDuckValidator;
    private final Validator<FlyingDuck> flyingDuckValidator;
    private final Validator<FlyingAndSwimmingDuck> flyingAndSwimmingDuckValidator;

    private final List<Observer<User>> observers = new ArrayList<>();

    @Override
    public void addObserver(Observer<User> e) {
        observers.add(e);
    }

    @Override
    public void removeObserver(Observer<User> e) {
        observers.remove(e);
    }

    @Override
    public void notifyObservers(User t) {
        observers.forEach(x -> x.update(this, t));
    }

    /**
     * Constructor care primeste dependentele (Dependency Injection).
     */
    public UserService(UserRepositoryInterface userRepository,
                       Validator<Persoana> pv, Validator<SwimmingDuck> swv, Validator<FlyingDuck> flv, Validator<FlyingAndSwimmingDuck> flvd) {
        this.userRepository = userRepository;
        this.persoanaValidator = pv;
        this.swimmingDuckValidator = swv;
        this.flyingDuckValidator = flv;
        this.flyingAndSwimmingDuckValidator = flvd;
    }

    // --- METODE CRUD ---

    /**
     * Adauga sau actualizeaza un utilizator.
     * Include validarea polimorfica si hashing-ul parolei.
     */
    @Override
    public void addUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Utilizatorul nu poate fi null.");
        }

        // 1. Validarea (Polimorfica, folosind Validatorul specific)
        if (user instanceof Persoana p) {
            persoanaValidator.validate(p);
        } else if (user instanceof FlyingAndSwimmingDuck fsd) {
            flyingAndSwimmingDuckValidator.validate(fsd);
        } else if (user instanceof SwimmingDuck sd) {
            swimmingDuckValidator.validate(sd);
        } else if (user instanceof FlyingDuck fd) {
            flyingDuckValidator.validate(fd);
        } else {
            throw new IllegalArgumentException("Tip de utilizator necunoscut.");
        }

        // 2. Hashing (Responsabilitatea Service-ului: Securitate)
        // Hasham parola inainte de a o salva
        user.setPasswordHash(PasswordHasher.hashPassword(user.getPasswordHash()));

        // 3. Persistenta (Delegare catre Repository)
        Optional<User> result = userRepository.save(user);

        notifyObservers(user);

        // Notificare in caz de update (optional)
        if (result.isPresent()) {
            System.out.println("DEBUG: Utilizator existent cu ID " + user.getId() + " a fost actualizat.");
        }
    }

    /**
     * Sterge un utilizator dupa ID.
     */
    @Override
    public void deleteUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID-ul nu poate fi null.");
        }

        Optional<User> deletedUser = userRepository.delete(id);

        // Logica de Business: Arunca exceptie daca nu a fost gasit
        if (deletedUser.isEmpty()) {
            throw new EntityNotFoundException("Nu a fost gasit utilizatorul cu ID-ul: " + id);
        }
        notifyObservers(deletedUser.get());
    }

    /**
     * Gaseste un utilizator dupa ID.
     */
    @Override
    public Optional<User> findUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID-ul nu poate fi null.");
        }
        return userRepository.findOne(id);
    }

    /**
     * Returneaza toti utilizatorii.
     */
    @Override
    public Iterable<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Simuleaza o metoda de business complexa.
     */
    public String displayUserInfo(Long id) {
        Optional<User> userOpt = findUser(id);

        if (userOpt.isEmpty()) {
            throw new EntityNotFoundException("Utilizatorul cu ID " + id + " nu exista in retea.");
        }

        User user = userOpt.get();
        return "Info User: Username=" + user.getUsername() + ", Email=" + user.getEmail();
    }

    // --- METODE DE FILTRARE SI PAGINARE ---

    @Override
    public List<Duck> findAllDucks() {
        // Delegare directa catre Repository
        return userRepository.findAllDucks();
    }

    @Override
    public List<Duck> findDucksByType(String userType) {
        if (userType == null || "Toate".equalsIgnoreCase(userType)) {
            // Daca nu exista filtru, returneaza toate ratele
            return userRepository.findAllDucks();
        }

        // Delegare directa pentru tipuri specifice
        return userRepository.findDucksByType(userType);
    }

    /**
     * Obtine o pagina de utilizatori, gestionand paginarea si filtrarea dupa tip.
     * Deleaga logica de paginare si filtrare eficienta direct Repository-ului (SQL LIMIT/OFFSET).
     */
    @Override
    public Page<User> findUsersPaginatedAndFiltered(Pageable pageable, String filterType) {
        // Delegare directa catre Repository (presupunand ca Repository-ul gestioneaza eficient I/O)
        return userRepository.findUsersPaginatedAndFiltered(pageable, filterType);
    }

    // --- METODE DE AUTENTIFICARE ---

    /**
     * Autentifica un utilizator.
     * @param username Numele de utilizator.
     * @param password Parola introdusa.
     * @return Obiectul User autentificat.
     * @throws AuthenticationException Daca autentificarea esueaza.
     */
    public User login(String username, String password) throws AuthenticationException {

        // 1. Cauta utilizatorul dupa username (delegare catre Repository)
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            // Nu dezvaluim daca a gresit username-ul sau parola, pastram mesajul generic
            throw new AuthenticationException("Username sau parola incorecta.");
        }

        User user = userOpt.get();

        // 2. Verifica hash-ul parolei (Responsabilitatea Service-ului: Securitate)
        // Compara parola introdusa cu hash-ul stocat in baza de date
        if (!PasswordHasher.checkPassword(password, user.getPasswordHash())) {
            throw new AuthenticationException("Username sau parola incorecta.");
        }

        // 3. Autentificare Succes
        user.login(); // Marcheaza userul ca logat (logica de domeniu)
        return user;
    }

    public void updateOnlyPhoto(Long id, String photoPath) {
        // Aici NU se mai face hashing, deoarece parola nu este trimisă spre DB
        userRepository.updateProfileImage(id, photoPath);

        // Dacă folosești Observer, nu uita să notifici observatorii aici

        notifyObservers(userRepository.findOne(id).orElse(null));
    }

    @Override
    public List<User> getAll() {
        // Converteste rezultatul din repository (Iterable) în Listă
        List<User> list = new ArrayList<>();
        userRepository.findAll().forEach(list::add);
        return list;
    }
}