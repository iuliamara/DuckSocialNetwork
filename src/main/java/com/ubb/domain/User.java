package com.ubb.domain;

import java.util.Objects;

/**
 * Clasa abstracta de baza pentru un utilizator al sistemului.
 * Extinde Entity cu ID de tip Long.
 * Implementeaza interfata Observer pentru a primi notificari de la subiecte (Subject).
 */
public abstract class User extends Entity<Long> implements Observer<String> {
    private String username;
    private String email;
    private String passwordHash; // Nume redenumit pentru a reflecta stocarea hash-ului

    private String imagePath;

    private boolean isLoggedIn;

    // --- Specificatii: Constructor ---

    /**
     * Constructor cu parametrii pentru crearea unui nou utilizator.
     * Starea initiala de logare este 'false'.
     * @param username Numele unic al utilizatorului.
     * @param email Adresa de email a utilizatorului.
     * @param passwordHash Hash-ul parolei utilizatorului.
     */
    public User(String username, String email, String passwordHash){
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isLoggedIn = false;
    }

    // --- Specificatii: Getters & Setters ---

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returneaza hash-ul parolei (NU parola in text clar).
     * @return Hash-ul parolei.
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Seteaza noul hash al parolei.
     * @param passwordHash Noul hash al parolei.
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    // --- Specificatii: Metode generale (Operatii Simple) ---

    /**
     * Seteaza starea de logare a utilizatorului la 'true'.
     */
    public void login(){
        this.isLoggedIn = true;
    }

    /**
     * Seteaza starea de logare a utilizatorului la 'false'.
     */
    public void logout(){
        this.isLoggedIn = false;
    }

    // --- Specificatii: Contractul Object ---

    @Override
    public String toString() {
        // NU includem hash-ul parolei in toString din motive de securitate.
        return "User{" + "ID=" + getId() + ", username='" + username + "', email='" + email + "'}";
    }

    // --- Specificatii: Implementarea Interfetei Observer ---

    /**
     * Metoda apelata de subiect (Subject) pentru a notifica acest Observer (User).
     * @param observable Subiectul care a generat notificarea.
     * @param message Mesajul specific al notificarii.
     */
    @Override
    public void update(Subject<String> observable, String message){
        // Aici NU se folosesc stream-uri, deoarece este o singura operatie de I/O (System.out.println).
        // Stream-urile sunt utile pentru prelucrarea colectiilor (liste, set-uri, etc.).

        String notificare;

        if (observable instanceof Event event) {
            // Verificare si extragere detalii daca subiectul este de tip Event.
            notificare = "NOTIFICARE USER " + this.getUsername() +
                    " (ID " + this.getId() + "): Evenimentul '" +
                    event.getName() + "' s-a incheiat. Mesaj: " + message;
        } else {
            // Notificare generica.
            notificare = "NOTIFICARE USER " + this.getUsername() +
                    ": S-a primit o notificare cu mesajul: " + message;
        }

        System.out.println(notificare);
    }
    @Override
    public boolean equals(Object o) {
        // 1. Verificam identitatea
        if (this == o) return true;

        // 2. Verificam daca o este null sau daca tipurile difera
        if (o == null || getClass() != o.getClass()) return false;

        // 3. Facem cast si comparam campurile relevante (ID-ul)
        User user = (User) o;

        // Verificam ID-ul si ne asiguram ca nu e null
        return Objects.equals(this.getId(), user.getId());
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    @Override
    public int hashCode() {
        return Objects.hash(this.getId());
    }
}