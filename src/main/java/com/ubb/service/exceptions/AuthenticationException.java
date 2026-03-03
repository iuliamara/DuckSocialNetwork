package com.ubb.service.exceptions;

// O extindere a RuntimeException este recomandată pentru excepțiile de Service,
// deoarece ele nu trebuie să fie declarate în semnătura metodei (unchecked).

public class AuthenticationException extends RuntimeException {

    /**
     * Constructor care primește un mesaj de eroare.
     * @param message Descrierea erorii (ex: "Username sau parolă incorectă.").
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Constructor care primește un mesaj și cauza (dacă există o excepție subiacentă).
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}