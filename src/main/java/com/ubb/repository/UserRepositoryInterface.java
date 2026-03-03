package com.ubb.repository;

import com.ubb.domain.User;
import com.ubb.domain.Duck;
import com.ubb.utils.paging.Page;
import com.ubb.utils.paging.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Interfata specifica pentru Repository-ul de Utilizatori (User).
 * Extinde PagingRepository pentru a include functionalitatea de paginare si CRUD.
 */
public interface UserRepositoryInterface extends PagingRepository<Long, User>{

    /**
     * Cauta si returneaza o lista de Rate (Duck) filtrate dupa un anumit tip
     * (de exemplu, SwimmingDuck sau FlyingDuck).
     * @param type Tipul de rata (clasa) dupa care se filtreaza.
     * @return Lista de Rate care se potrivesc tipului.
     */
    List<Duck> findDucksByType(String type);

    /**
     * Cauta si returneaza toate Ratele (Duck) din depozit.
     * @return Lista care contine toate entitatile de tip Duck.
     */
    List<Duck> findAllDucks();

    /**
     * Obtine o pagina de Utilizatori filtrati pe baza unui criteriu specificat.
     * @param pageable Obiectul Pageable care contine pagina si marimea.
     * @param filterType Criteriul de filtrare (ex: tipul de user, rolul).
     * @return Un obiect Page<User> care contine continutul paginii si metadatele de paginare.
     */
    Page<User> findUsersPaginatedAndFiltered(Pageable pageable, String filterType);

    /**
     * Cauta un utilizator dupa username. Necesar pentru login sau validarea unicitatii.
     * @param username Username-ul de cautat.
     * @return Optional<User> daca utilizatorul este gasit, Optional.empty() altfel.
     */
    Optional<User> findByUsername(String username);

    void updateProfileImage(Long userId, String newImagePath);
}