package com.ubb.utils.paging;

import java.util.List;

/**
 * Clasa generica care contine rezultatele paginarii si metadatele (numar total de pagini/elemente).
 * Aceasta este obiectul returnat de metodele de paginare din Repository/Service.
 *
 * @param <E> Tipul entitatii continute in pagina (ex: User, FriendshipDTO)
 */
public class Page<E> {
    private final List<E> content;
    private final int pageNumber; // Pagina curenta (de la 1 in sus)
    private final int totalPages;
    private final long totalElements; // Numarul total de elemente din colectia initiala

    /**
     * Constructor pentru obiectul Page.
     */
    public Page(List<E> content, int pageNumber, int totalPages, long totalElements) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    // --- Getters ---

    /**
     * Returneaza lista de entitati continute in aceasta pagina.
     */
    public List<E> getContent() { return content; }

    /**
     * Returneaza numarul paginii curente (incepand cu 1).
     */
    public int getPageNumber() { return pageNumber; }

    /**
     * Returneaza numarul total de pagini.
     */
    public int getTotalPages() { return totalPages; }

    /**
     * Returneaza numarul total de elemente din intreaga colectie.
     */
    public long getTotalElements() { return totalElements; }

    // --- Metode helper pentru GUI (Logica de navigare) ---

    /**
     * Verifica daca exista o pagina urmatoare.
     */
    public boolean hasNext() {
        // Exista o pagina urmatoare daca numarul total de pagini > 0 si pagina curenta nu este ultima
        return totalPages > 0 && pageNumber < totalPages;
    }

    /**
     * Verifica daca exista o pagina anterioara.
     */
    public boolean hasPrevious() {
        // Exista o pagina anterioara daca pagina curenta nu este prima
        return pageNumber > 1;
    }
}