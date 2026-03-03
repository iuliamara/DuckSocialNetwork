package com.ubb.repository;

import com.ubb.domain.Entity;
import com.ubb.utils.paging.Pageable;

import java.util.List;

/**
 * Interfata care extinde Repository si adauga metodele necesare pentru paginare.
 * Defineste contractul pentru lucrul cu date in pagini (Pagination).
 *
 * @param <ID> Tipul ID-ului
 * @param <E> Tipul Entitatii
 */
public interface PagingRepository<ID , E extends Entity<ID>> extends Repository<ID, E> {

    /**
     * Obtine o pagina de entitati pe baza parametrilor Pageable.
     * Implementarea bazei de date (DB) va folosi clauzele LIMIT si OFFSET.
     *
     * @param pageable Obiectul Pageable care contine numarul paginii dorite (pageNumber) si marimea paginii (pageSize).
     * @return O Lista de entitati pentru pagina solicitata.
     */
    List<E> findAll(Pageable pageable);

    /**
     * Calculeaza numarul total de elemente din Repository (Total Elements).
     * Aceasta metoda este utilizata pentru a calcula numarul total de pagini.
     *
     * @return Numarul total de inregistrari (lungimea totala a colectiei).
     */
    long count();

    // NOTA: Metoda size() din Repository poate fi folosita alternativ in loc de count()
    // daca ambele returneaza lungimea totala a colectiei.
}