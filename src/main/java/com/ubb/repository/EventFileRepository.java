package com.ubb.repository;

import com.ubb.domain.Event;
import com.ubb.domain.RaceEvent;
import com.ubb.domain.User;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.exceptions.RepositoryException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Repository specific pentru entitatile de tip Event, care persista datele in fisier.
 * Extinde AbstractFileRepository si implementeaza Template Methods pentru I/O.
 */
public class EventFileRepository extends AbstractFileRepository<Long, Event> implements EventRepositoryInterface{
    private static final String SEPARATOR = ";";
    private static final String SUBSCRIBER_SEPARATOR = ",";

    /**
     * Constructor.
     * @param filename Numele fisierului de persistenta.
     * @param validator Validatorul pentru entitati de tip Event.
     */
    public EventFileRepository(String filename, Validator<Event> validator) {
        super(filename, validator);
    }

    // --- Specificatii: Hook-uri si Metode Auxiliare (cu Stream-uri) ---

    /**
     * Hook method: Metoda apelata inainte de salvare, folosita aici pentru a genera ID-ul.
     * @param entity Entitatea care urmeaza sa fie salvata.
     * @return Entitatea cu ID-ul setat (daca era null).
     */
    @Override
    protected Event preSaveHook(Event entity) {
        // Verificam daca ID-ul trebuie generat (pentru Event nou)
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

    /**
     * Metoda publica pentru Service: Returneaza ID-urile abonatilor pentru un eveniment.
     * Utilizeaza harta auxiliara generica din clasa de baza.
     * @param eventId ID-ul evenimentului.
     * @return Lista de ID-uri Long ale abonatilor.
     */
    public List<Long> getSubscriberIdsForEvent(Long eventId) {
        // Necesita castToID din clasa de baza deoarece cheia hartii este ID generic
        return auxiliaryMemberMap.getOrDefault(castToID(eventId), Collections.emptyList());
    }

    public List<Event> findFinishedEventsByUserId(Long userId){
        return entities.values().stream()
                .filter(Event::isFinished) // 1. Selectăm doar evenimentele marcate cu is_finished = true
                .filter(event -> {
                    // 2. Verificăm dacă utilizatorul se află în harta de abonați
                    List<Long> subscriberIds = getSubscriberIdsForEvent(event.getId());
                    return subscriberIds.contains(userId);
                })
                .collect(Collectors.toList());
    }

    // --- Specificatii: Template Methods (I/O cu Stream-uri) ---

    /**
     * Implementarea Template Method: Extrage entitatea Event dintr-un rand de text.
     */
    @Override
    protected Event extractEntity(String line) {
        try {
            String[] parts = line.split(SEPARATOR);
            if (parts.length < 3) throw new RepositoryException("Linie incompleta pentru Event.");

            Long id = Long.parseLong(parts[0].trim());
            String typeEvent = parts[1].trim();
            String name = parts[2].trim();
            // Campurile pot lipsi, de aceea se verifica parts.length
            String subscribersStr = parts.length > 3 ? parts[3].trim() : "";
            String distancesStr = parts.length > 4 ? parts[4].trim() : "";

            // Parsarea ID-urilor abonatilor (OPERATIE PE STREAM-URI)
            List<Long> subscriberIds = Arrays.stream(subscribersStr.split(SUBSCRIBER_SEPARATOR))
                    .filter(s -> !s.isEmpty())
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            // Stocheaza ID-urile in harta auxiliara a parintelui
            putAuxiliaryData(id, subscriberIds);

            Event event;
            // Instantierea evenimentului concret
            if (typeEvent.equalsIgnoreCase("RaceEvent")) {

                // Parsarea Distantelor Balizelor (OPERATIE PE STREAM-URI)
                List<Double> beaconDistances = new ArrayList<>();
                if (!distancesStr.isEmpty()) {
                    beaconDistances = Arrays.stream(distancesStr.split(SUBSCRIBER_SEPARATOR))
                            .filter(s -> !s.isEmpty())
                            .map(String::trim)
                            .map(Double::parseDouble)
                            .collect(Collectors.toList());
                }

                // Numarul de participanti necesar este dimensiunea listei de balize
                int numParticipants = beaconDistances.size();

                // Instantiem RaceEvent
                event = new RaceEvent(name,
                        numParticipants,
                        Collections.emptyList(), // Participantii eligibili se stabilesc in Service
                        beaconDistances);
            } else {
                throw new RepositoryException("Tip de Eveniment necunoscut: " + typeEvent);
            }

            event.setId(id);
            return event;

        } catch (Exception e) {
            throw new RepositoryException("Eroare la parsarea entitatii Event: " + e.getMessage());
        }
    }

    /**
     * Implementarea Template Method: Creeaza un rand de text dintr-o entitate Event.
     */
    @Override
    protected String createEntityAsString(Event entity) {
        // Extrage ID-urile abonatilor (OPERATIE PE STREAM-URI)
        List<Long> updatedSubscriberIds = entity.getSubscribers().stream()
                .filter(obs -> obs instanceof User) // Filtrare pentru siguranță
                .map(obs -> ((User) obs).getId()) // Cast si extragere ID
                .collect(Collectors.toList());

        // Convertește lista de ID-uri in string (OPERATIE PE STREAM-URI)
        String subscriberIdsStr = updatedSubscriberIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(SUBSCRIBER_SEPARATOR));

        // Sincronizeaza harta auxiliara cu lista abonatilor actuala (pentru a fi disponibila imediat)
        putAuxiliaryData(entity.getId(), updatedSubscriberIds);

        // Campuri de baza (ID;TYPE;NAME;SUBS_LIST)
        String baseString = entity.getId() + SEPARATOR +
                entity.getClass().getSimpleName() + SEPARATOR +
                entity.getName() + SEPARATOR +
                subscriberIdsStr;

        // Serializare specifica RaceEvent
        if (entity instanceof RaceEvent raceEvent) {

            // Extrage distantele balizelor (OPERATIE PE STREAM-URI)
            String distancesStr = raceEvent.getBeaconDistances().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(SUBSCRIBER_SEPARATOR));

            // Adauga noul camp la final (SUBS_LIST;DIST_LIST)
            return baseString + SEPARATOR + distancesStr;
        }

        // Returneaza stringul de baza pentru evenimente simple
        return baseString;
    }
}