package com.ubb.service.exceptions;

/**
 * Exceptie aruncata cand o entitate nu poate fi gasita (ex: utilizatorul cu ID-ul dat nu exista).
 */
public class EntityNotFoundException extends ServiceException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}