package com.ubb.domain;

import java.time.LocalDateTime;
import java.util.Objects;
/**
 * Entitatea care reprezinta o relatie de prietenie bidirectionala.
 * ID-ul este de tip Tuple<Long, Long> (cheie compusa).
 *
 * ID-urile in Tuple sunt stocate in ordine stricta (ID mic, ID mare) pentru a asigura
 * unicitatea in Repository (relatia A-B este tratata la fel ca B-A).
 */
public class Friendship extends Entity<Tuple<Long, Long>> {
    private LocalDateTime friendsFrom; // Data de la care sunt prieteni

    /**
     * Seteaza ID-ul in forma standardizata (ID mic, ID mare).
     * Aceasta este o operatie de "mapping" sau "pipeline" interna.
     */
    private void standardizeId(Long idUser1, Long idUser2) {
        // Logica de validare esentiala. Validari complete se fac in Validator.
        if (idUser1 == null || idUser2 == null || Objects.equals(idUser1, idUser2)) {
            throw new IllegalArgumentException("ID-urile nu pot fi nule sau egale.");
        }

        // Aici se aplica o logica de "pipeline" (desi imperativa) care standardizeaza cheia
        // pentru a mentine unicitatea (A-B == B-A).
        if (idUser1 < idUser2) {
            // Ordinea standardizata (ID mic, ID mare)
            setId(new Tuple<>(idUser1, idUser2));
        } else {
            // Inversam ID-urile pentru a obtine ordinea standardizata
            setId(new Tuple<>(idUser2, idUser1));
        }
    }

    /**
     * Constructor pentru crearea unei noi prietenii (folosind data curenta).
     * @param idUser1 ID-ul primului utilizator.
     * @param idUser2 ID-ul celui de-al doilea utilizator.
     */
    public Friendship(Long idUser1, Long idUser2) {
        standardizeId(idUser1, idUser2);
        this.friendsFrom = LocalDateTime.now();
    }

    /**
     * Constructor pentru reconstructia prieteniei din Repository (cu data specificata).
     * @param idUser1 ID-ul primului utilizator.
     * @param idUser2 ID-ul celui de-al doilea utilizator.
     * @param friendsFrom Data de la care sunt prieteni.
     */
    public Friendship(Long idUser1, Long idUser2, LocalDateTime friendsFrom) {
        standardizeId(idUser1, idUser2);
        this.friendsFrom = friendsFrom;
    }

    // --- Specificatii: Getters & Setters ---

    /**
     * Returneaza data de la care exista prietenia.
     * @return Data in format LocalDateTime.
     */
    public LocalDateTime getFriendsFrom() {
        return friendsFrom;
    }

    /**
     * Seteaza data de la care exista prietenia.
     * Folosita tipic de Repository la extragerea datelor din baza de date.
     * @param friendsFrom Data in format LocalDateTime.
     */
    public void setFriendsFrom(LocalDateTime friendsFrom) {
        this.friendsFrom = friendsFrom;
    }

    /**
     * Returneaza ID-ul primului utilizator din Tuple (cel mai mic ID).
     * @return ID-ul utilizatorului 1.
     */
    public Long getIdUser1() {
        return getId().getFirst();
    }

    /**
     * Returneaza ID-ul celui de-al doilea utilizator din Tuple (cel mai mare ID).
     * @return ID-ul utilizatorului 2.
     */
    public Long getIdUser2() {
        return getId().getSecond();
    }

    // --- Specificatii: Contractul Object ---

    /**
     * Returneaza o reprezentare a obiectului Friendship.
     * Afiseaza doar data, nu si ora (pentru simplitate).
     * @return String-ul reprezentativ.
     */
    @Override
    public String toString() {
        return String.format("Friendship{user1Id=%d, user2Id=%d, friendsFrom=%s}",
                getIdUser1(), getIdUser2(), friendsFrom.toLocalDate());
    }

    // Metodele equals() si hashCode() sunt mostenite din Entity<Tuple<Long, Long>>
}