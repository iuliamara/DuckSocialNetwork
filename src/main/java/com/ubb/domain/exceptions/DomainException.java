package com.ubb.domain.exceptions;

/**
 * Exceptia de baza (ROOT) pentru toate exceptiile de business/domeniu din aplicatie.
 * Extinde RuntimeException (unchecked).
 */
public class DomainException extends RuntimeException {
    public DomainException() {
        super();
    }

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public DomainException(Throwable cause) {
        super(cause); // Apeleaza constructorul RuntimeException(Throwable)
    }
}