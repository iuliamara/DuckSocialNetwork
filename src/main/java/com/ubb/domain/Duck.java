package com.ubb.domain;

/**
 * Clasa abstracta de baza pentru o Rata (Duck) din sistem.
 * Extinde User si adauga atribute specifice performantei fizice (viteza, rezistenta).
 * Aceste atribute sunt necesare pentru calculul performantei medii (Card).
 */
public abstract class Duck extends User {

    // Atribute comune necesare pentru calculul performantei medii
    protected double viteza;
    protected double rezistenta;

    /**
     * Constructor cu parametrii pentru crearea unei Rate.
     * @param username Numele de utilizator.
     * @param email Adresa de email.
     * @param passwordHash Hash-ul parolei.
     * @param viteza Valoarea vitezei Ratei.
     * @param rezistenta Valoarea rezistentei Ratei.
     */
    public Duck(String username, String email, String passwordHash, double viteza, double rezistenta) {
        super(username, email, passwordHash);
        this.viteza = viteza;
        this.rezistenta = rezistenta;
    }

    // --- Specificatii: Getters & Setters ---

    /**
     * Returneaza viteza ratei.
     * @return Viteza.
     */
    public double getViteza() {
        return viteza;
    }

    /**
     * Seteaza viteza ratei.
     * @param viteza Noua viteza.
     */
    public void setViteza(double viteza) {
        this.viteza = viteza;
    }

    /**
     * Returneaza rezistenta ratei.
     * @return Rezistenta.
     */
    public double getRezistenta() {
        return rezistenta;
    }

    /**
     * Seteaza rezistenta ratei.
     * @param rezistenta Noua rezistenta.
     */
    public void setRezistenta(double rezistenta) {
        this.rezistenta = rezistenta;
    }

    // --- Specificatii: Comportament specific ---

    /**
     * Comportament: Genereaza si trimite un mesaj automat (ex: "Quack! Am terminat antrenamentul!").
     * Simuleaza trimiterea unui mesaj fara destinatar explicit (posibil catre toti prietenii).
     * Logica de creare a entitatii Message si de trimitere in retea va fi in Service.
     */
    public void sendAutomaticMessage() {
        // Logica Service va prelua acest apel si va crea entitatea Message.
        String automaticContent = "Quack! Am terminat antrenamentul!";
        // Acest mesaj este trimis in Service folosind this.getId().
    }

    // --- Specificatii: Contractul Object ---

    /**
     * Returneaza o reprezentare a obiectului Duck.
     * Include ID-ul si atributele specifice de performanta.
     * @return String-ul reprezentativ.
     */
    @Override
    public String toString() {
        return "Duck{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", viteza=" + viteza +
                ", rezistenta=" + rezistenta +
                '}';
    }
}