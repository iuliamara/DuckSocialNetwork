package com.ubb.domain;


import com.ubb.domain.capabilities.Inotator;

/**
 * Clasa care reprezinta o Rata Inotatoare (SwimmingDuck).
 * Extinde Duck si implementeaza interfata Inotator.
 * Mosteneste atributele de performanta (viteza, rezistenta) si functionalitatile de User.
 */
public class SwimmingDuck  extends Duck implements Inotator {

    /**
     * Constructor pentru initializarea Ratei Inotatoare.
     * @param username Numele de utilizator.
     * @param email Adresa de email.
     * @param passwordHash Hash-ul parolei.
     * @param viteza Valoarea vitezei.
     * @param rezistenta Valoarea rezistentei.
     */
    public SwimmingDuck(String username, String email, String passwordHash, double viteza, double rezistenta) {
        super(username, email, passwordHash, viteza, rezistenta);
    }

    // --- Specificatii: Implementarea Interfetei Inotator ---

    /**
     * Define actiunea specifica Ratei Inotatoare.
     * Afiseaza actiunea de inot impreuna cu viteza specifica.
     * Aici NU se folosesc stream-uri, fiind o operatie de I/O simpla.
     */
    @Override
    public void inoata() {
        // Utilizam concatenare simpla (fara String.format pentru a evita adaugarea unei noi metode)
        System.out.println(getUsername() + " Inoata cu o viteza de " + viteza);
    }

    // --- Specificatii: Contractul Object ---

    /**
     * Returneaza o reprezentare a obiectului SwimmingDuck.
     * Include ID-ul, username-ul si atributele specifice de performanta.
     * @return String-ul reprezentativ.
     */
    @Override
    public String toString() {
        return "SwimmingDuck{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", viteza=" + getViteza() +
                ", rezistenta=" + getRezistenta() +
                '}';
    }
}