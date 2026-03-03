package com.ubb.domain.validation;

import com.ubb.domain.FlyingAndSwimmingDuck;
import com.ubb.domain.exceptions.ValidationException;

/**
 * Validator specific for FlyingAndSwimmingDuck.
 * Inherits validation logic for base User fields and common Duck performance fields.
 */
public class FlyingAndSwimmingDuckValidator extends DuckBaseValidator<FlyingAndSwimmingDuck> {

    @Override
    public void validate(FlyingAndSwimmingDuck duck) throws ValidationException {
        // 1. Validarea campurilor User de baza (Username, Email, Password)
        // (Metoda mostenita din UserValidator)
        super.validateBaseUserFields(duck);

        // 2. Validarea campurilor Duck (Viteza, Rezistenta)
        // (Metoda mostenita din DuckBaseValidator)
        super.validateDuckPerformanceFields(duck);
    }
}