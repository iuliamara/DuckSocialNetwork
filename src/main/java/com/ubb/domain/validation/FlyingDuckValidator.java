package com.ubb.domain.validation;

import com.ubb.domain.FlyingDuck;
import com.ubb.domain.exceptions.ValidationException;

public class FlyingDuckValidator extends DuckBaseValidator<FlyingDuck> {

    @Override
    public void validate(FlyingDuck duck) throws ValidationException {
        // Validarea campurilor User de baza
        super.validateBaseUserFields(duck);

        // Validarea campurilor Duck (Viteza, Rezistenta)
        super.validateDuckPerformanceFields(duck);

    }
}