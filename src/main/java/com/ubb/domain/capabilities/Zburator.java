package com.ubb.domain.capabilities;

/**
 * Interfata functionala care defineste capacitatea unei entitati de a zbura.
 * Entitatile care implementeaza aceasta interfata trebuie sa furnizeze
 * o implementare specifica pentru actiunea de zbor.
 */
public interface Zburator {
    /**
     * Define actiunea de a zbura.
     */
    void zboara();
}