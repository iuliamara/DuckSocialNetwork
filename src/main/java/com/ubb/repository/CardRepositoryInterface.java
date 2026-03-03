package com.ubb.repository;

import com.ubb.domain.Card;
import com.ubb.domain.Duck;
import java.util.List;

/**
 * Interfata specifica pentru Repository-ul de Carduri.
 * Extinde interfața generica Repository pentru a mosteni contractul CRUD,
 * si adauga metode necesare logicii de reconstructie M:N (Card <-> Duck).
 */
public interface CardRepositoryInterface extends Repository<Long, Card<? extends Duck>> {

    // --- METODA ADITIONALA (SPECIFICA RECONSTRUCTIEI) ---

    /**
     * Obtine lista de ID-uri de Duck asociate unui Card.
     * Aceasta metoda este esentiala pentru a reconstrui relatia M:N a Card-ului
     * dupa incarcarea din depozitul de date (fisier sau baza de date).
     *
     * @param cardId ID-ul Cardului.
     * @return Lista de ID-uri Long ale membrilor (Duck-urilor) din Card.
     */
    List<Long> getMemberIdsForCard(Long cardId);

    // NOTA: Metodele findOne, save, delete, findAll, size sunt mostenite automat
    // de la interfața Repository<Long, Card<? extends Duck>>.
}