package com.ubb.repository;

import com.ubb.domain.Entity;
import java.util.Optional;

/**
 * Interfata generica pentru Repository.
 * Define Contractul de Persistenta (CRUD) pentru entitatile din aplicatie.
 *
 * @param <ID> Tipul ID-ului (ex: Long, Tuple<Long, Long>)
 * @param <E> Tipul Entitatii (trebuie sa extinda Entity<ID>)
 */
public interface Repository<ID, E extends Entity<ID>> {

    /**
     * Cauta si returneaza o entitate dupa ID-ul specificat.
     * @param id ID-ul entitatii cautate.
     * @return Optional<E> care contine entitatea, sau Optional.empty() daca nu exista.
     */
    Optional<E> findOne(ID id);

    /**
     * Returneaza toate entitatile din depozit.
     * @return Iterable<E> care contine toate entitatile.
     */
    Iterable<E> findAll();

    /**
     * Salveaza entitatea data.
     * Entitatea nu trebuie sa aiba deja un ID in depozit.
     * @param entity Entitatea care trebuie salvata.
     * @return Optional<E> care contine entitatea salvata, sau Optional.empty() daca a fost salvata cu succes.
     * Returneaza Optional<E> care contine entitatea existenta daca entitatea data nu a putut fi salvata (e.g., ID deja existent).
     */
    Optional<E> save(E entity);

    /**
     * Sterge entitatea cu ID-ul specificat.
     * @param id ID-ul entitatii care trebuie stearsa.
     * @return Optional<E> care contine entitatea stearsa, sau Optional.empty() daca nu exista o entitate cu ID-ul dat.
     */
    Optional<E> delete(ID id);

    /**
     * Returneaza numarul total de entitati din depozit.
     * @return Numarul de entitati.
     */
    long size();
}