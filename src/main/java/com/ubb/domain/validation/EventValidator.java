package com.ubb.domain.validation;

import com.ubb.domain.Event;
import com.ubb.domain.exceptions.ValidationException;

/**
 * Validator pentru entitatea Event.
 */
public class EventValidator implements Validator<Event> {

    @Override
    public void validate(Event event) throws ValidationException {
        String errors = "";

        // 1. Validare Nume Eveniment
        if (event.getName() == null || event.getName().trim().isEmpty()) {
            errors += "Numele evenimentului nu poate fi gol.\n";
        }

        // 2. Validare ID (Aceasta verificare este optionala in Domain, dar ajuta la detectarea erorilor)
        // Deoarece ne bazam pe Repository pentru a genera ID-ul, o lasam goala pentru entitatile noi.
        // Verificam doar daca numele este corect.

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}