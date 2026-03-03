package com.ubb.domain.validation;

import com.ubb.domain.Friendship;
import com.ubb.domain.exceptions.ValidationException;

public class FriendshipValidator implements Validator<Friendship> {

    @Override
    public void validate(Friendship entity) throws ValidationException {
        String errors = "";

        //Verifica ID-urile nule
        if (entity.getIdUser1() == null || entity.getIdUser2() == null) {
            errors += "ID-urile utilizatorilor in relatia de prietenie nu pot fi nule!\n";
        }

        //Verifica prietenia cu sine
        if (entity.getIdUser1() != null && entity.getIdUser1().equals(entity.getIdUser2())) {
            errors += "Un utilizator nu poate fi prieten cu el insusi!\n";
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}