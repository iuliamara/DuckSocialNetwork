package com.ubb.utils.dto;

import java.time.LocalDateTime;

/**
 * Clasa Data Transfer Object (DTO) utilizata pentru a transfera informatiile complete
 * despre o prietenie (ID-uri, Username-uri si data) de la Service catre UI.
 * Acest format este optimizat pentru afisarea in tabele (ex: JavaFX PropertyValueFactory).
 */
public class FriendshipDTO {
    private final Long idUser1;
    private final String username1;
    private final Long idUser2;
    private final String username2;
    private final LocalDateTime friendsFrom;

    /**
     * Constructor pentru FriendshipDTO.
     */
    public FriendshipDTO(Long idUser1, String username1, Long idUser2, String username2, LocalDateTime friendsFrom) {
        this.idUser1 = idUser1;
        this.username1 = username1;
        this.idUser2 = idUser2;
        this.username2 = username2;
        this.friendsFrom = friendsFrom;
    }

    // --- Getters (Necesare pentru afisare/binding in UI) ---

    public Long getIdUser1() { return idUser1; }
    public String getUsername1() { return username1; }
    public Long getIdUser2() { return idUser2; }
    public String getUsername2() { return username2; }
    public LocalDateTime getFriendsFrom() { return friendsFrom; }

    /**
     * Returneaza o reprezentare a DTO-ului.
     */
    @Override
    public String toString() {
        return "FriendshipDTO{" +
                "user1Id=" + idUser1 +
                ", username1='" + username1 + '\'' +
                ", user2Id=" + idUser2 +
                ", username2='" + username2 + '\'' +
                ", friendsFrom=" + friendsFrom.toLocalDate() +
                '}';
    }
}