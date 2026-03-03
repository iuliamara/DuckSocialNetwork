package com.ubb.domain;

import java.time.LocalDate;
/**
 * Clasa concreta care reprezinta o Persoana (utilizator uman) din sistem.
 * Extinde User si adauga atribute personale (nume, prenume, dataNasterii, ocupatie, nivelEmpatie).
 */
public class Persoana extends User {
    private String nume;
    private String prenume;
    private LocalDate dataNasterii;
    private String ocupatie;

    private int nivelEmpatie;

    /**
     * Constructor pentru initializarea obiectului Persoana.
     * @param username Numele de utilizator.
     * @param email Adresa de email.
     * @param passwordHash Hash-ul parolei.
     * @param nume Numele de familie.
     * @param prenume Prenumele.
     * @param dataNasterii Data nasterii.
     * @param ocupatie Ocupatia.
     * @param nivelEmpatie Nivelul de empatie (intreg).
     */
    public Persoana(String username, String email, String passwordHash, String nume, String prenume, LocalDate dataNasterii, String ocupatie, int nivelEmpatie) {
        super(username, email, passwordHash); // Apel la constructorul User
        this.nume = nume;
        this.prenume = prenume;
        this.dataNasterii = dataNasterii;
        this.ocupatie = ocupatie;
        this.nivelEmpatie = nivelEmpatie;
    }

    // --- Specificatii: Getters & Setters ---

    public String getNume() {return nume;}
    public void setNume(String nume) {this.nume = nume;}
    public String getPrenume() {return prenume;}
    public void setPrenume(String prenume) {this.prenume = prenume;}
    public LocalDate getDataNasterii() {return dataNasterii;}
    public void setDataNasterii(LocalDate dataNasterii) {this.dataNasterii = dataNasterii;}
    public String getOcupatie() {return ocupatie;}
    public void setOcupatie(String ocupatie) {this.ocupatie = ocupatie;}
    public int getNivelEmpatie() {return nivelEmpatie;}
    public void setNivelEmpatie(int nivelEmpatie) {this.nivelEmpatie = nivelEmpatie;}

    // --- Specificatii: Contractul Object ---

    /**
     * Returneaza o reprezentare a obiectului Persoana.
     * NU include data nasterii pentru concizie.
     * @return String-ul reprezentativ.
     */
    @Override
    public String toString() {
        return "Persoana{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", nume='" + nume + '\'' +
                ", prenume='" + prenume + '\'' +
                ", ocupatie='" + ocupatie + '\'' +
                ", empatie=" + nivelEmpatie +
                '}';
    }
}