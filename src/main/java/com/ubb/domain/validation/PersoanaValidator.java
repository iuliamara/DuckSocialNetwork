// com/ubb/domain/validation/PersoanaValidator.java
package com.ubb.domain.validation;

import com.ubb.domain.Persoana;
import com.ubb.domain.exceptions.ValidationException;
import java.time.LocalDate;

public class PersoanaValidator extends UserValidator<Persoana> {

    @Override
    public void validate(Persoana persoana) throws ValidationException {
        // Apelam validarea campurilor comune
        super.validateBaseUserFields(persoana);

        // Validari specifice Persoana
        String errors = "";

        if (persoana.getNume() == null || persoana.getNume().trim().isEmpty()) {
            errors += "Numele nu poate fi gol.\n";
        }
        if (persoana.getPrenume() == null || persoana.getPrenume().trim().isEmpty()) {
            errors += "Prenumele nu poate fi gol.\n";
        }

        // Data de nastere nu poate fi in viitor
        if (persoana.getDataNasterii().isAfter(LocalDate.now())) {
            errors += "Data nasterii nu poate fi in viitor.\n";
        }

        // Nivelul Empatiei trebuie sa fie in intervalul [1, 10]
        int nivelEmpatie = persoana.getNivelEmpatie();
        if (nivelEmpatie < 1 || nivelEmpatie > 10) {
            errors += "Nivelul de Empatie trebuie sa fie intre 1 si 10.\n";
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}