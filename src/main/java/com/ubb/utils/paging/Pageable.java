package com.ubb.utils.paging;

/**
 * Clasa care defineste cererea de paginare (Pagination Request).
 * Contine informatiile necesare pentru a extrage o anumita pagina de date.
 */
public class Pageable {
    private final int pageNumber;
    private final int pageSize;

    /**
     * Constructor.
     * @param pageNumber Pagina dorita (incepe de la 1)
     * @param pageSize Numarul maxim de elemente pe pagina
     */
    public Pageable(int pageNumber, int pageSize) {
        // Logica de validare (desi ar trebui sa fie in Service/Validator):
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Numarul paginii (pageNumber) trebuie sa fie cel putin 1.");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("Marimea paginii (pageSize) trebuie sa fie cel putin 1.");
        }

        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    /**
     * Returneaza numarul paginii solicitate (incepe de la 1).
     */
    public int getPageNumber() { return pageNumber; }

    /**
     * Returneaza marimea paginii (numarul maxim de elemente).
     */
    public int getPageSize() { return pageSize; }

}