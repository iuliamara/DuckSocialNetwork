package com.ubb.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class FriendshipRequest extends Entity<Long> {
    private final User fromUser;
    private final User toUser;
    private RequestStatus status;
    private LocalDateTime dateSent;
    // Urmareste daca destinatarul a vazut notificarea
    private boolean isViewedByRecipient;

    // --- CONSTRUCTORI ---
    public FriendshipRequest(Long id, User fromUser, User toUser, RequestStatus status, LocalDateTime dateSent,boolean isViewedByRecipient) {
        super(id);
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.status = status;
        this.dateSent = dateSent;
        this.isViewedByRecipient = isViewedByRecipient;
    }

    public FriendshipRequest(User fromUser, User toUser) {
        super();
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.status = RequestStatus.PENDING;
        this.dateSent = LocalDateTime.now();
        this.isViewedByRecipient = false; // Notif noua
    }

    // --- GETTERS & SETTERS ---
    public User getFromUser() {
        return fromUser;
    }
    public User getToUser() {
        return toUser;
    }
    public RequestStatus getStatus() {
        return status;
    }
    public LocalDateTime getDateSent() {
        return dateSent;
    }

    public void setDateSent(LocalDateTime dateSent) {
        this.dateSent=dateSent;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public boolean isViewedByRecipient() {
        return isViewedByRecipient;
    }

    public void setViewedByRecipient(boolean viewedByRecipient) {
        isViewedByRecipient = viewedByRecipient;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        // Compara pe baza ID-ului MOsTENIT (daca exista)
        FriendshipRequest that = (FriendshipRequest) o;
        return Objects.equals(this.getId(), that.getId()); // Foloseste getId() mostenit
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fromUser, toUser, status, dateSent);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return "FriendshipRequest{" +
                "id=" + getId() +
                ", fromUser=" + fromUser.getUsername() +
                ", toUser=" + toUser.getUsername() +
                ", status=" + status +
                ", dateSent=" + dateSent.format(formatter) +
                '}';
    }
}


