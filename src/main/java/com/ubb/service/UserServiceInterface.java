package com.ubb.service;

import com.ubb.domain.Duck;
import com.ubb.domain.Subject;
import com.ubb.domain.User;
import com.ubb.service.exceptions.AuthenticationException;
import com.ubb.service.exceptions.EntityNotFoundException;
import com.ubb.domain.exceptions.ValidationException;
import com.ubb.utils.paging.Page;
import com.ubb.utils.paging.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserServiceInterface extends Subject<User> {

    /**
     * Adauga un utilizator nou in sistem.
     * @param user Utilizatorul de adaugat.
     * @throws ValidationException daca datele utilizatorului sunt invalide.
     */
    void addUser(User user);

    /**
     * Sterge un utilizator dupa ID.
     * @param id ID-ul utilizatorului de sters.
     * @throws EntityNotFoundException daca utilizatorul nu este gasit.
     */
    void deleteUser(Long id);

    /**
     * Gaseste un utilizator dupa ID.
     * @param id ID-ul utilizatorului.
     * @return Optional care contine utilizatorul sau Optional.empty().
     */
    Optional<User> findUser(Long id);

    /**
     * Returneaza toti utilizatorii.
     */
    Iterable<User> findAllUsers();

    public List<Duck> findAllDucks();

    List<Duck> findDucksByType(String userType);

    public Page<User> findUsersPaginatedAndFiltered(Pageable pageable, String filterType);

    User login(String username, String password) throws AuthenticationException;

    void updateOnlyPhoto(Long id, String photoPath);

    List<User> getAll();
}