package com.ubb.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clasa generica (pereche) pentru a reprezenta o colectie ordonata de doua elemente.
 * Este ideala pentru a implementa ID-uri compuse (chei primare).
 * Clasa a fost facuta imutabila prin marcarea campurilor ca 'final' si eliminarea set-urilor (setter-ilor).
 *
 * @param <E1> Tipul primei componente
 * @param <E2> Tipul celei de-a doua componente
 */
public class Tuple<E1, E2> implements Serializable {
    // Campurile sunt private si finale pentru a asigura imutabilitatea
    private final E1 first;
    private final E2 second;

    /**
     * Constructor pentru initializarea tuplei.
     * @param first Prima componenta.
     * @param second A doua componenta.
     */
    public Tuple(E1 first, E2 second) {
        this.first = first;
        this.second = second;
    }

    // --- Specificatii: Getters (Setterii au fost eliminati) ---

    /**
     * Returneaza prima componenta.
     * @return Prima componenta de tip E1.
     */
    public E1 getFirst() {
        return first;
    }

    /**
     * Returneaza a doua componenta.
     * @return A doua componenta de tip E2.
     */
    public E2 getSecond() {
        return second;
    }

    // --- Specificatii: Contractul Object ---

    /**
     * Returneaza reprezentarea in format String a tuplei.
     * @return String-ul tuplei.
     */
    @Override
    public String toString() {
        return "Tuple [first=" + first + ", second=" + second + "]";
    }

    /**
     * Verifica egalitatea pe baza ambelor componente.
     * Doua tuple sunt egale DACA au aceeasi clasa SI ambele componente sunt egale.
     * @param o Obiectul de comparat.
     * @return true daca tuplele sunt egale, false altfel.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        // Verificarea de tip.
        if (o == null || getClass() != o.getClass()) return false;

        Tuple<?, ?> tuple = (Tuple<?, ?>) o;

        // Doua tuple sunt egale daca si numai daca ambele componente sunt egale.
        // Se foloseste Objects.equals pentru a gestiona NULL-urile in componente.
        return Objects.equals(first, tuple.first) && Objects.equals(second, tuple.second);
    }

    /**
     * Genereaza un cod hash bazat pe ambele componente.
     * @return Codul hash.
     */
    @Override
    public int hashCode() {
        // Hash code bazat pe ambele elemente.
        return Objects.hash(first, second);
    }
}