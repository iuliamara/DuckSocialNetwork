package com.ubb.domain.capabilities;

/**
 * Interfata functionala care defineste capacitatea unei entitati de a inota.
 * Entitatile care implementeaza aceasta interfata trebuie sa furnizeze
 * o implementare specifica pentru actiunea de inot.
 */
public interface Inotator {
    /**
     * Define actiunea de a inota.
     */
    void inoata();
}