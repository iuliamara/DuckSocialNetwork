package com.ubb.repository.exceptions;

import com.ubb.domain.exceptions.DomainException;

/**
 * Exceptie pentru erorile legate de operatiunile de persistenta (Repository).
 * Poate semnala erori I/O, probleme de formatare in fisier etc.
 */
public class RepositoryException extends DomainException {

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepositoryException(Throwable cause) {
        super(cause);
    }
}