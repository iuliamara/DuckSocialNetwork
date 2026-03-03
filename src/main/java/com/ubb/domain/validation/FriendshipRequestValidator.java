package com.ubb.domain.validation;

import com.ubb.domain.FriendshipRequest;
import com.ubb.domain.exceptions.ValidationException;
import com.ubb.repository.FriendshipRequestRepositoryInterface;
import com.ubb.domain.User;
import com.ubb.domain.Entity;

public class FriendshipRequestValidator implements Validator<FriendshipRequest> {
    @Override
    public void validate(FriendshipRequest entity) throws ValidationException {
        if (entity == null) {
            throw new ValidationException("Cererea de prietenie nu poate fi nula.");
        }

        String errors = "";

        // 1. Validare Useri (referintele la User nu sunt nule)
        User fromUser = entity.getFromUser();
        User toUser = entity.getToUser();

        if (fromUser == null || fromUser.getId() == null) {
            errors += "Expeditorul (fromUser) este invalid sau nu are ID (entitate nesalvata).\n";
        }
        if (toUser == null || toUser.getId() == null) {
            errors += "Destinatarul (toUser) este invalid sau nu are ID (entitate nesalvata).\n";
        }

        // Daca userii sunt valiți, putem verifica diferența.
        if (errors.isEmpty()) {
            // 2. Logica de domeniu: Userul nu poate trimite o cerere catre el insusi.
            if (fromUser.getId().equals(toUser.getId())) {
                errors += "Expeditorul si destinatarul trebuie sa fie utilizatori diferiti.\n";
            }
        }

        // 3. Validare Status si Data
        if (entity.getStatus() == null) {
            errors += "Statusul cererii nu poate fi null.\n";
        }
        if (entity.getDateSent() == null) {
            errors += "Data trimiterii cererii nu poate fi null.\n";
        }


        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
