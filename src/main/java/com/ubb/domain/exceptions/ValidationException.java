package com.ubb.domain.exceptions;

/**
 * Exceptie aruncata de clasa Validator cand datele entitatii sunt incorecte.
 */
public class ValidationException extends DomainException {
    public ValidationException() {
        super();
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}