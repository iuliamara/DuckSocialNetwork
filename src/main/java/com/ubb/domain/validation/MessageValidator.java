package com.ubb.domain.validation;

import com.ubb.domain.Message;
import com.ubb.domain.exceptions.ValidationException;

import java.util.ArrayList;
import java.util.List;

public class MessageValidator implements Validator<Message> {

    @Override
    public void validate(Message entity) throws ValidationException {
        List<String> errors = new ArrayList<>();

        // 1. Validare Sender
        if (entity.getFrom() == null || entity.getFrom().getId() == null) {
            errors.add("Sender-ul (Utilizatorul expeditor) trebuie să fie specificat și să aibă un ID valid.");
        }

        // 2. Validare Destinatari
        if (entity.getTo() == null || entity.getTo().isEmpty()) {
            errors.add("Lista de destinatari nu poate fi goală.");
        } else {
            // Verifică dacă lista conține ID-uri valide
            entity.getTo().stream()
                    .filter(user -> user.getId() == null)
                    .findFirst()
                    .ifPresent(user -> errors.add("Toți destinatarii trebuie să aibă un ID valid."));
        }

        // 3. Validare Conținut
        if (entity.getMessage() == null || entity.getMessage().trim().isEmpty()) {
            errors.add("Conținutul mesajului nu poate fi gol.");
        }

        // 4. Validare Mesaj Răspuns (Reply)
        // Dacă este un răspuns, trebuie să existe un mesaj părinte valid.
        if (entity.getReply() != null && entity.getReply().getId() == null) {
            errors.add("Dacă este un răspuns, mesajul părinte trebuie să aibă un ID valid.");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(String.join("\n", errors));
        }
    }
}