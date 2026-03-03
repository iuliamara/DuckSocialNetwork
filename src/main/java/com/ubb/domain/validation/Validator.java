package com.ubb.domain.validation;

import com.ubb.domain.exceptions.ValidationException;
/**
 * Interfata Strategy. Defineste un contract comun pentru toti algoritmii de validare.
 */
public interface Validator<E> {
    /**
     * Valideaza o entitate.
     * @param entity entitatea de validat
     * @throws ValidationException daca entitatea nu este valida
     */
    void validate(E entity) throws ValidationException;
}
