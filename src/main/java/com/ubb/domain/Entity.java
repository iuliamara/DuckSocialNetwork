package com.ubb.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clasa abstracta de baza pentru toate entitatile din aplicatie.
 * Asigura ca fiecare entitate are un identificator unic (ID)
 * si implementeaza corect contractul Object.equals si Object.hashCode
 * bazat pe acest ID.
 *
 * @param <ID> Tipul identificatorului (de exemplu, Long, Integer, String)
 */
public abstract class Entity<ID> implements Serializable {

    private ID id;

    /**
     * Constructor implicit. ID-ul este initial null pana la salvarea in Repository.
     */
    public Entity() {
        this.id = null;
    }

    /**
     * Constructor pentru reconstructie/initializare cu un ID specific.
     * @param id Identificatorul unic.
     */
    public Entity(ID id) {
        this.id = id;
    }

    // --- Specificatii: Getteri si Setteri ---

    /**
     * Returneaza identificatorul unic al entitatii.
     * @return ID-ul entitatii.
     */
    public ID getId() {
        return id;
    }

    /**
     * Seteaza identificatorul unic al entitatii.
     * @param id Noul ID.
     */
    public void setId(ID id) {
        this.id = id;
    }

    // --- Specificatii: Contractul Object ---

    /**
     * Compara aceasta entitate cu un alt obiect pentru egalitate.
     * Egalitatea este definita strict pe baza ID-ului si a clasei.
     * Doua entitati sunt egale DACA au acelasi ID SI apartin aceleiasi clase.
     * O entitate fara ID NU este considerata egala cu alta.
     *
     * @param o Obiectul de comparat.
     * @return true daca obiectele sunt egale, false altfel.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        // Inlocuieste 'instanceof Entity<?> entity' cu o forma clasica, mai explicita.
        if (o == null || getClass() != o.getClass()) return false;

        // Cast-ul este sigur la acest punct, deoarece getClass() a fost verificat.
        Entity<?> entity = (Entity<?>) o;

        // O entitate fara ID nu poate fi egala cu alta.
        if (id == null || entity.id == null) {
            return false;
        }

        // Egalitatea se bazeaza exclusiv pe ID-ul entitatii.
        // Utilizam Objects.equals pentru a gestiona NULL inainte de check-ul 'id == null'
        // chiar daca am facut deja un check, e o practica buna (desi redundant aici).
        return Objects.equals(id, entity.id);
    }

    /**
     * Returneaza un cod hash bazat exclusiv pe ID-ul entitatii.
     * Daca ID-ul este null, returneaza 0.
     *
     * @return Codul hash.
     */
    @Override
    public int hashCode() {
        // Hash code bazat exclusiv pe ID.
        // Daca ID-ul este null, hash-ul este 0.
        return Objects.hash(id);
    }
}