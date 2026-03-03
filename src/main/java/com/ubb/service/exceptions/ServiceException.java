package com.ubb.service.exceptions;

import com.ubb.domain.exceptions.DomainException;

/**
 * Exceptia de baza pentru toate erorile de logica de business care apar in stratul de Service.
 */
public class ServiceException extends DomainException {
    public ServiceException() {
        super();
    }

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceException(Throwable cause) {
        super(cause);
    }
}