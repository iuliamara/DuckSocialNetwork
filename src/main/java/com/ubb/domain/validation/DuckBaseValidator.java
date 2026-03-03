package com.ubb.domain.validation;

import com.ubb.domain.Duck;
import com.ubb.domain.exceptions.ValidationException;

// Mosteneste de la UserValidator pentru a prelua validarile de baza
public abstract class DuckBaseValidator<T extends Duck> extends UserValidator<T> {

    /**
     * Valideaza campurile comune de performanta ale oricarei rate.
     */
    protected void validateDuckPerformanceFields(Duck duck) throws ValidationException {
        String errors = "";

        if (duck.getViteza() <= 0) {
            errors += "Viteza ratei trebuie sa fie o valoare strict pozitiva.\n";
        }
        if (duck.getRezistenta() <= 0) {
            errors += "Rezistenta ratei trebuie sa fie o valoare strict pozitiva.\n";
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}