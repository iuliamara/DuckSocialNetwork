package com.ubb.domain;

import com.ubb.domain.capabilities.Zburator;

/**
 * Clasa care reprezinta o Rata Zburatoare (FlyingDuck).
 * Extinde Duck si implementeaza interfata Zburator.
 * Mosteneste atributele de performanta (viteza, rezistenta) si functionalitatile de User.
 */
public class FlyingDuck extends Duck implements Zburator {

    /**
     * Constructor pentru initializarea Ratei Zburatoare.
     * @param username Numele de utilizator.
     * @param email Adresa de email.
     * @param passwordHash Hash-ul parolei.
     * @param viteza Valoarea vitezei.
     * @param rezistenta Valoarea rezistentei.
     */
    public FlyingDuck(String username, String email, String passwordHash, double viteza, double rezistenta) {
        super(username, email, passwordHash, viteza, rezistenta);
    }

    // --- Specificatii: Implementarea Interfetei Zburator ---

    /**
     * Define actiunea specifica Ratei Zburatoare.
     * Afiseaza actiunea de zbor impreuna cu rezistenta specifica.
     * Aici NU se folosesc stream-uri, fiind o operatie de I/O simpla.
     */
    @Override
    public void zboara() {
        // Utilizam concatenare simpla (fara String.format pentru a evita adaugarea unei noi metode)
        System.out.println(getUsername() + " zboara cu rezistenta " + rezistenta);
    }

    // --- Specificatii: Contractul Object ---

    /**
     * Returneaza o reprezentare a obiectului FlyingDuck.
     * Include ID-ul, username-ul si atributele specifice de performanta.
     * @return String-ul reprezentativ.
     */
    @Override
    public String toString() {
        return "FlyingDuck{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", viteza=" + getViteza() +
                ", rezistenta=" + getRezistenta() +
                '}';
    }
}