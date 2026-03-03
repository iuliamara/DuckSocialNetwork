package com.ubb.domain.validation;

import com.ubb.domain.SwimmingDuck;
import com.ubb.domain.exceptions.ValidationException;

public class SwimmingDuckValidator extends DuckBaseValidator<SwimmingDuck> {

    @Override
    public void validate(SwimmingDuck duck) throws ValidationException {
        // Validarea campurilor User de baza (Username, Email, Password)
        super.validateBaseUserFields(duck);

        // Validarea campurilor Duck (Viteza, Rezistenta)
        super.validateDuckPerformanceFields(duck);

    }
}