package com.ubb.service;

import com.ubb.domain.Card;
import com.ubb.domain.Duck;
import com.ubb.service.exceptions.EntityNotFoundException;
import com.ubb.domain.exceptions.ValidationException; // Adaugat, presupunand ca e necesar

import java.util.List;

/**
 * Interfata care defineste contractul pentru serviciile legate de entitatea Card.
 * Gestioneaza logica de business, validarea si delegarea catre Repository.
 */
public interface CardServiceInterface {

    /**
     * Adauga un nou card in retea.
     * @param card Cardul de adaugat.
     * @throws ValidationException Daca datele cardului sunt invalide.
     */
    void addCard(Card<? extends Duck> card);

    /**
     * Adauga un membru (o rata) intr-un card existent.
     * Include verificari de existenta si de tip.
     * @param cardId ID-ul cardului.
     * @param duckId ID-ul ratei de adaugat.
     * @throws EntityNotFoundException Daca cardul sau rata nu exista.
     * @throws ValidationException Daca rata nu este de tipul corect pentru card.
     */
    void addMembru(Long cardId, Long duckId);

    /**
     * Calculeaza si returneaza performanta medie a unui card.
     * @param cardId ID-ul cardului.
     * @return Performanta medie (Double).
     * @throws EntityNotFoundException Daca cardul nu exista.
     */
    double getPerformantaMedie(Long cardId);

    /**
     * Returneaza toate cardurile din sistem.
     * @return O colectie iterabila de Carduri.
     */
    Iterable<Card<? extends Duck>> findAllCards();

    /**
     * Returneaza lista de membri (Duck) a unui Card specific.
     * Metoda este utilizata pentru a reconstrui complet Cardul.
     * @param cardId ID-ul cardului.
     * @return Lista de obiecte Duck.
     * @throws EntityNotFoundException Daca cardul nu exista.
     */
    List<? extends Duck> getCardMembers(Long cardId);
}