package com.ubb.repository;

import com.ubb.domain.*;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.exceptions.RepositoryException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository specific pentru entitatile de tip Card, care persista datele in fisier.
 * Extinde AbstractFileRepository si implementeaza Template Methods pentru I/O.
 */
public class CardFileRepository extends AbstractFileRepository<Long, Card<? extends Duck>> implements CardRepositoryInterface {

    private static final String SEPARATOR = ";";
    private static final String MEMBER_SEPARATOR = ","; // Separator pentru lista de membri


    /**
     * Constructor.
     * @param fileName Numele fisierului de persistenta.
     * @param validator Validatorul pentru entitati de tip Card.
     */
    public CardFileRepository(String fileName, Validator<Card<? extends Duck>> validator) {
        super(fileName, validator);
    }

    // --- Specificatii: Hook-uri si Metode Auxiliare (cu Stream-uri) ---

    @Override
    protected Card<? extends Duck> preSaveHook(Card<? extends Duck> entity) {
        // Verificam daca ID-ul trebuie generat
        if (entity.getId() == null) {
            entity.setId(getNextId());
        }
        return entity;
    }

    /**
     * Metoda privata care gaseste urmatorul ID liber folosind operatii pe stream-uri.
     * @return Urmatorul ID Long disponibil.
     */
    private Long getNextId() {
        if (entities.isEmpty()) {
            return 1L;
        }

        // OPERATII PE STREAM-URI: Gasirea ID-ului maxim (inlocuieste un for)
        return entities.keySet().stream()
                .filter(Objects::nonNull)
                .mapToLong(id -> id)
                .max()
                .orElse(0L) + 1;
    }

    // --- Specificatii: Template Methods (I/O cu Stream-uri) ---

    /**
     * Implementarea Template Method: Extrage entitatea Card dintr-un rand de text.
     * Format asteptat: ID;TipCard;NumeCard;ID_Membru1,ID_Membru2,...
     *
     * @param line Linia de text din fisier.
     * @return Entitatea Card extrasa.
     */
    @Override
    protected Card<? extends Duck> extractEntity(String line) {
        try {
            String[] parts = line.split(SEPARATOR);
            if (parts.length < 3) {
                throw new RepositoryException("Linie incompleta sau format invalid pentru Card.");
            }

            Long id = Long.parseLong(parts[0].trim());
            String tipCard = parts[1].trim(); // Ex: "SwimmingCard"
            String numeCard = parts[2].trim();
            String membriStr = parts.length > 3 ? parts[3].trim() : "";

            // Parsarea ID-urilor membrilor (OPERATIE PE STREAM-URI)
            List<Long> memberIds = Arrays.stream(membriStr.split(MEMBER_SEPARATOR))
                    .filter(s -> !s.isEmpty())
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            // Utilizeaza metoda parintelui pentru a stoca datele auxiliare (M:N)
            // Cheia este ID generic (Long in acest caz), valoarea este List<Long>
            putAuxiliaryData(id, memberIds);

            Card<? extends Duck> card;

            // Logica de Deserializare pentru tipuri concrete
            if (tipCard.equalsIgnoreCase("SwimmingCard")) {
                card = new SwimmingCard(numeCard);
            } else if (tipCard.equalsIgnoreCase("FlyingCard")) {
                card=new FlyingCard(numeCard);
            } else {
                throw new RepositoryException("Tip de Card necunoscut: " + tipCard);
            }

            card.setId(id);

            // Lista de membri din obiectul Card ramane goala in acest moment.
            // Reconstructia se face in Service folosind getMemberIdsForCard.
            return card;

        } catch (NumberFormatException e) {
            throw new RepositoryException("Eroare la parsarea ID-ului pentru Card: " + e.getMessage());
        } catch (Exception e) {
            throw new RepositoryException("Eroare la crearea entitatii Card: " + e.getMessage());
        }
    }

    /**
     * Implementarea Template Method: Creeaza un rand de text dintr-o entitate Card.
     * Format de scriere: ID;TipCard;NumeCard;ID_Membru1,ID_Membru2,...
     *
     * @param entity Entitatea Card de serializat.
     * @return String-ul serializat.
     */
    @Override
    protected String createEntityAsString(Card<? extends Duck> entity) {

        String tipCard = entity.getClass().getSimpleName();

        // Extrage ID-urile ratelor (membri) si le uneste in string (OPERATIE PE STREAM-URI)
        String membriIds = entity.getMembri().stream()
                .map(Duck::getId)
                .map(String::valueOf)
                // Utilizeaza Collectors.joining pentru o operatie concisa
                .collect(Collectors.joining(MEMBER_SEPARATOR));

        // Serializare
        return entity.getId() + SEPARATOR +
                tipCard + SEPARATOR +
                entity.getNumeCard() + SEPARATOR +
                membriIds;
    }

    // --- Specificatii: Implementarea CardRepositoryInterface ---

    /**
     * Obtine lista de ID-uri de Duck asociate unui Card, folosind harta auxiliara a parintelui.
     * @param cardId ID-ul Cardului.
     * @return Lista de ID-uri Long ale membrilor (Duck-urilor) din Card.
     */
    @Override
    public List<Long> getMemberIdsForCard(Long cardId) {
        // ID-ul cardId este de tip Long, care este compatibil cu ID generic al parintelui.
        return auxiliaryMemberMap.getOrDefault(cardId, Collections.emptyList());
    }
}