package com.ubb.service;

import com.ubb.domain.Friendship;
import com.ubb.domain.Subject;
import com.ubb.domain.User;
import com.ubb.service.exceptions.EntityNotFoundException;
import com.ubb.service.exceptions.FriendshipAlreadyExistsException;
import com.ubb.service.exceptions.ServiceException;
import com.ubb.utils.dto.FriendshipDTO;
import com.ubb.utils.paging.Page;
import com.ubb.utils.paging.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Interfata care defineste contractul pentru serviciile legate de entitatea Friendship.
 * Gestioneaza logica de business si validarea prieteniilor (relatie bidirectionala).
 */
public interface FriendshipServiceInterface extends Subject<Friendship> {

    /**
     * Creeaza si adauga o noua prietenie intre doi utilizatori.
     * @param idUser1 ID-ul primului utilizator.
     * @param idUser2 ID-ul celui de-al doilea utilizator.
     * @throws EntityNotFoundException Daca unul dintre ID-uri nu exista in sistem.
     * @throws FriendshipAlreadyExistsException Daca prietenia exista deja (relatia este bidirectionala).
     */
    void addFriendship(Long idUser1, Long idUser2);

    /**
     * Sterge o prietenie intre doi utilizatori, indiferent de ordinea ID-urilor.
     * @param idUser1 ID-ul primului utilizator.
     * @param idUser2 ID-ul celui de-al doilea utilizator.
     * @throws EntityNotFoundException Daca prietenia nu este gasita.
     */
    void deleteFriendship(Long idUser1, Long idUser2);

    /**
     * Gaseste o prietenie dupa ID-urile celor doi utilizatori.
     * @param idUser1 ID-ul primului utilizator.
     * @param idUser2 ID-ul celui de-al doilea utilizator.
     * @return Optional care contine prietenia sau Optional.empty().
     */
    Optional<Friendship> findFriendship(Long idUser1, Long idUser2);

    /**
     * Returneaza toate prieteniile din sistem.
     * @return O colectie iterabila de Friendship.
     */
    Iterable<Friendship> findAllFriendships();

    /**
     * Returneaza o pagina de obiecte Friendship, mapate la formatul DTO, pentru afisare.
     * @param pageable Obiectul Pageable care contine pagina dorita si marimea paginii.
     * @return Un obiect Page<FriendshipDTO> care contine continutul paginii si metadatele.
     */
    Page<FriendshipDTO> findAllPaginated(Pageable pageable);

    /**
     * Returneaza lista de prieteni ai unui anumit utilizator, pe baza obiectului User.
     * @param user Utilizatorul al carui prieteni sunt cautati.
     * @return Lista de obiecte User care sunt prieteni cu user-ul dat.
     * @throws ServiceException Daca utilizatorul nu exista.
     */
    List<User> getFriendsOfUser(User user) throws ServiceException;

    /**
     * Supraincarcare (overload) folosind ID-ul (mai comuna).
     * @param userId ID-ul utilizatorului.
     * @return Lista de obiecte User care sunt prieteni.
     */
    List<User> getFriendsOfUser(Long userId) throws ServiceException;

    /**
     * Returneaza lista de DTO-uri care reprezinta toate prieteniile stabilite de un user.
     */
    List<FriendshipDTO> getFriendshipDTOs(Long userId) throws ServiceException;

    Page<FriendshipDTO> findAllPaginatedForUser(Pageable pageable, Long userId);

    boolean areFriends(Long id1, Long id2);

    /**
     * Returnează o listă cu toți utilizatorii care sunt prieteni cu utilizatorul dat.
     */
    java.util.List<User> getAllFriends(Long userId);
}