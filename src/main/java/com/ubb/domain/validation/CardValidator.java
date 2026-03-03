package com.ubb.domain.validation;

import com.ubb.domain.Card;
import com.ubb.domain.Duck;
import com.ubb.domain.exceptions.ValidationException;

/**
 * Validator generic pentru entitatea Card.
 * Asigura ca atributele de baza ale cardului sunt valide.
 * @param <E> Tipul de Duck continut in Card.
 */
public class CardValidator<E extends Duck> implements Validator<Card<E>> {

    @Override
    public void validate(Card<E> card) throws ValidationException {
        String errors = "";

        // Validare Nume Card
        if (card.getNumeCard() == null || card.getNumeCard().trim().isEmpty()) {
            errors += "Numele cardului nu poate fi gol.\n";
        }

        // Validare Membri (O regula de business: un card nu poate fi gol)
//        if (card.getMembri().isEmpty()) {
//             errors += "Un card trebuie sa contina cel putin o rata.\n";
//        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}