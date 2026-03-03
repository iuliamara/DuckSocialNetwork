// com/ubb/domain/FlyingAndSwimmingDuck.java

package com.ubb.domain;

import com.ubb.domain.capabilities.Inotator;
import com.ubb.domain.capabilities.Zburator;

/**
 * Rata concreta care implementeaza ambele capabilitati: Inotator si Zburator.
 * Mosteneste atributele de performanta (viteza, rezistenta) si functionalitatile de User.
 */
public class FlyingAndSwimmingDuck extends Duck implements Inotator, Zburator {

    /**
     * Constructor pentru initializarea Ratei Zburatoare si Inotatoare.
     * @param username Numele de utilizator.
     * @param email Adresa de email.
     * @param passwordHash Hash-ul parolei.
     * @param viteza Valoarea vitezei.
     * @param rezistenta Valoarea rezistentei.
     */
    public FlyingAndSwimmingDuck(String username, String email, String passwordHash, double viteza, double rezistenta) {
        super(username, email, passwordHash, viteza, rezistenta);
    }

    // --- Specificatii: Implementarea Interfetei Inotator ---

    /**
     * Define actiunea specifica de inot.
     * Aici NU se folosesc stream-uri, fiind o operatie de I/O simpla.
     */
    @Override
    public void inoata() {
        System.out.println(getUsername() + " inoata si se antreneaza pentru cursa!");
    }

    // --- Specificatii: Implementarea Interfetei Zburator ---

    /**
     * Define actiunea specifica de zbor.
     * Aici NU se folosesc stream-uri, fiind o operatie de I/O simpla.
     */
    @Override
    public void zboara() {
        System.out.println(getUsername() + " zboara in formatie perfecta!");
    }

    // --- Specificatii: Contractul Object ---

    /**
     * Returneaza o reprezentare a obiectului FlyingAndSwimmingDuck.
     * Include ID-ul, username-ul si atributele specifice de performanta.
     * @return String-ul reprezentativ.
     */
    @Override
    public String toString() {
        return "FlyingAndSwimmingDuck{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", viteza=" + getViteza() +
                ", rezistenta=" + getRezistenta() +
                '}';
    }
}