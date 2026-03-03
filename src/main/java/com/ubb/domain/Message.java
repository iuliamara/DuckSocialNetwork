package com.ubb.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Message extends Entity<Long> {
    private final User from;
    private final List<User> to;
    private final String message;
    private final LocalDateTime data;

    // NOU: Atributul optional pentru Reply, conform cerintei 1.
    // Va fi null pentru un mesaj nou creat.
    private Message reply;

    private boolean isRead;

    public Message(User from, List<User> to, String message) {
        this.from = from;
        this.to = to;
        this.message = message;
        this.data = LocalDateTime.now();
        this.reply = null;
        this.isRead = false; // Implicit este necitit la crearea unui mesaj nou
    }

    public Message(User from, List<User> to, String message, Message replyTo) {
        this.from = from;
        this.to = to;
        this.message = message;
        this.data = LocalDateTime.now();
        this.reply = replyTo;
        this.isRead = false;
    }

    public Message(User from, List<User> to, String message, LocalDateTime data, Message replyTo) {
        this.from = from;
        this.to = to;
        this.message = message;
        this.data = data;
        this.reply = replyTo;
    }

    public Message(User from, List<User> to, String message, LocalDateTime data, Message replyTo, boolean isRead) {
        this.from = from;
        this.to = to;
        this.message = message;
        this.data = data;
        this.reply = replyTo;
        this.isRead = isRead;
    }


    // --- Getteri ---

    public User getFrom() {
        return from;
    }

    public List<User> getTo() {
        return to;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getData() {
        return data;
    }

    public Message getReply() {
        return reply;
    }

    // Metodă utilă pentru a verifica dacă este un răspuns
    public boolean isReply() {
        return reply != null;
    }

    // Getter și Setter pentru isRead
    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
    // --- Override-uri (equals, hashCode, toString) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message message1)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(from, message1.from) && Objects.equals(to, message1.to) && Objects.equals(message, message1.message) && Objects.equals(data, message1.data) && Objects.equals(reply, message1.reply);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), from, to, message, data, reply);
    }

    @Override
    public String toString() {
        // Afișează destinația ca o listă de ID-uri/Username-uri pentru simplitate
        String replyInfo = (reply != null) ? " (Reply to ID: " + reply.getId() + ")" : "";

        return "Message{" +
                "id=" + getId() +
                ", from=" + from.getUsername() +
                ", to=[" + to.stream().map(User::getUsername).collect(Collectors.joining(", ")) +
                "], message='" + message + '\'' +
                ", data=" + data.format(DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy")) +
                replyInfo +
                '}';
    }
}