package com.ubb.service.exceptions;

/**
 * Exceptie aruncata cand se incearca adaugarea unei prietenii care exista deja.
 */
public class FriendshipAlreadyExistsException extends ServiceException {
    public FriendshipAlreadyExistsException(String message) {
        super(message);
    }

    public FriendshipAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}