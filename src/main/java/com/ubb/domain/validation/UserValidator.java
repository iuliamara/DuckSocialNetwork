package com.ubb.domain.validation;

import com.ubb.domain.User;
import com.ubb.domain.exceptions.ValidationException;

public abstract class UserValidator<T extends User> implements Validator<T> {

    protected void validateBaseUserFields(User user) throws ValidationException{
        String errors = "";

        //Validari comune (User)
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            errors += "Username-ul nu poate fi gol!\n";
        } else if (user.getUsername().length() < 3) {
            errors += "Username-ul trebuie sa aiba minim 3 caractere!\n";
        }

        if (user.getEmail() == null || !user.getEmail().contains("@") || !user.getEmail().contains(".")) {
            errors += "Email-ul este invalid!\n";
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().length() < 6) {
            errors += "Parola trebuie sa aiba minim 6 caractere!\n";
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    @Override
    public abstract void validate(T entity) throws ValidationException;
}