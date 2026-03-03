package com.ubb.domain;

import com.ubb.domain.exceptions.DomainException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Clasa abstracta generica pentru un Card (grup de rate).
 * Permite restrictionarea tipului de rate (ex: SwimMasters contine doar SwimmingDuck).
 * Extinde Entity cu ID de tip Long.
 *
 * @param <E> Tipul de rata, trebuie sa extinda Duck.
 */
public abstract class Card<E extends Duck> extends Entity<Long> implements Serializable {

    private final String numeCard;
    private final List<E> membri;

    /**
     * Constructor pentru initializarea Card-ului.
     * @param numeCard Numele cardului.
     */
    public Card(String numeCard) {
        this.numeCard = numeCard;
        this.membri = new ArrayList<>();
    }

    // --- Specificatii: Getters ---

    /**
     * Returneaza numele cardului.
     * @return Numele cardului.
     */
    public String getNumeCard() { return numeCard; }

    /**
     * Returneaza lista membrilor.
     * @return Lista de membri.
     */
    public List<E> getMembri() { return membri; }

    // --- Specificatii: Metode de Colectie (cu Stream-uri) ---

    /**
     * Adauga o rata in Card.
     * Utilizeaza operatii pe stream-uri pentru a verifica unicitatea.
     * @param duck Rata de adaugat.
     * @throws DomainException Daca rata este deja membra.
     */
    public void addMembru(E duck) {
        // OPERATIE PE STREAM-URI: Verifica unicitatea membrilor existenti comparand ID-urile
        boolean alreadyExists = membri.stream()
                .anyMatch(m -> Objects.equals(m.getId(), duck.getId()));

        if (alreadyExists) {
            throw new DomainException("Rata cu ID " + duck.getId() + " este deja membra a cardului.");
        }
        this.membri.add(duck);
    }

    /**
     * Calculeaza performanta medie a cardului.
     * Performanta medie este media aritmetica a vitezei medii si a rezistentei medii.
     * Utilizeaza operatii pe stream-uri pentru calculul sumelor.
     *
     * @return Performanta medie sub forma unui Double (0.0 daca lista este goala).
     */
    public double getPerformantaMedie() {
        if (membri.isEmpty()) {
            return 0.0;
        }

        // OPERATIE PE STREAM-URI: Calculul sumei vitezelor
        double totalViteza = membri.stream()
                .mapToDouble(Duck::getViteza)
                .sum();

        // OPERATIE PE STREAM-URI: Calculul sumei rezistentelor
        double totalRezistenta = membri.stream()
                .mapToDouble(Duck::getRezistenta)
                .sum();

        double size = membri.size();
        double medieViteza = totalViteza / size;
        double medieRezistenta = totalRezistenta / size;

        return (medieViteza + medieRezistenta) / 2.0;
    }

    /**
     * Adauga o rata in Card fara a verifica unicitatea (folosita la popularea initiala).
     * @param duck Rata de adaugat.
     */
    public void populateMembru(E duck) {
        this.membri.add(duck); // pentru populare silentioasa
    }

    /**
     * Goleate lista membrilor. Folosita la inceputul reconstructiei.
     */
    public void clearMembri() {
        this.membri.clear();
    }

    // --- Specificatii: Contractul Object ---

    /**
     * Returneaza o reprezentare a obiectului Card.
     * @return String-ul reprezentativ.
     */
    @Override
    public String toString() {
        return "Card{" +
                "id=" + getId() +
                ", numeCard='" + numeCard + '\'' +
                ", membri=" + membri.size() +
                '}';
    }
}