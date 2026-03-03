package com.ubb.service;

import com.ubb.domain.*;
import com.ubb.domain.Observer;
import com.ubb.domain.capabilities.Inotator;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.EventRepositoryInterface;
import com.ubb.repository.Repository;
import com.ubb.service.exceptions.EntityNotFoundException;
import com.ubb.service.exceptions.ServiceException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Serviciu care gestioneaza logica de business pentru entitatile Event.
 * Include operații CRUD, logica Observer Pattern si executie ASINCRONA conform Lab 10.
 */
public class EventService implements EventServiceInterface {

    private final EventRepositoryInterface eventRepository;
    private final Repository<Long, User> userRepository;
    private final Validator<Event> eventValidator;

    // ExecutorService necesar pentru a rula cursele asincron
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    // Lista de controllere (Observeri) care sunt online in acest moment
    private final List<Observer<Object>> uiObservers = new ArrayList<>();

    @Override
    public void addObserver(Observer<Object> observer) {
        uiObservers.add(observer);
    }

    @Override
    public void removeObserver(Observer<Object> observer) {
        uiObservers.remove(observer);
    }

    @Override
    public void notifyObservers(Object data) {
        // Notificam toate ferestrele deschise
        uiObservers.forEach(obs -> obs.update(this, data));
    }

    public EventService(Repository<Long, Event> eventRepository, Repository<Long, User> userRepository, Validator<Event> eventValidator) {
        this.eventRepository = (EventRepositoryInterface) eventRepository;
        this.userRepository = userRepository;
        this.eventValidator = eventValidator;
    }

    // --- LOGICA ASINCRONA (LAB 10) ---

    /**
     * Finalizeaza un eveniment in mod asincron folosind CompletableFuture.
     * Previne blocarea interfetei grafice in timpul calculelor complexe de clasament.
     */
    public CompletableFuture<String> finishEventAsync(Long eventId) {
        // Fără executorService ca parametru, folosește pool-ul global al sistemului
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1500);
                return finishEvent(eventId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceException("Executia asincrona a fost intrerupta.");
            }
        });
    }

    // --- LOGICA OBSERVER RECONSTRUCTION ---

    /**
     * Reconstruieste lista de observatori pe baza ID-urilor din tabela M:N.
     */
    private Event reconstructSubscribers(Event event) {
        List<Long> subscriberIds = eventRepository.getSubscriberIdsForEvent(event.getId());

        if (subscriberIds == null || subscriberIds.isEmpty()) {
            event.setSubscribers(Collections.emptyList());
            return event;
        }

        // Folosim stream-uri pentru a mapa ID-urile la obiecte User care sunt Observeri
        List<Observer<String>> reconstructedObservers = subscriberIds.stream()
                .map(userRepository::findOne)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(user -> user instanceof Observer)
                .map(user -> (Observer<String>) user)
                .collect(Collectors.toList());

        event.setSubscribers(reconstructedObservers);
        return event;
    }

    // --- OPERAȚII BUSINESS ---

    @Override
    public void createRaceEvent(String name, int numParticipants, List<Double> beaconDistances) {
        if (numParticipants <= 0 || beaconDistances.size() != numParticipants) {
            throw new ServiceException("Date invalide pentru crearea cursei.");
        }

        // Filtrare asincrona/stream a ratelor eligibile din sistem
        List<Duck> eligibleDucks = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .filter(user -> user instanceof Duck)
                .map(user -> (Duck) user)
                .collect(Collectors.toList());

        if (eligibleDucks.size() < numParticipants) {
            throw new ServiceException("Nu sunt suficiente rate pentru cursa.");
        }

        RaceEvent raceEvent = new RaceEvent(name, numParticipants, eligibleDucks, beaconDistances);
        addEvent(raceEvent);
    }

    @Override
    public void subscribe(Long eventId, Long userId) { // asta ar trebui asincron
        // Gasim evenimentul si reconstruim lista de abonati din DB
        Event event = eventRepository.findOne(eventId)
                .map(this::reconstructSubscribers)
                .orElseThrow(() -> new EntityNotFoundException("Eveniment negăsit."));

        User user = userRepository.findOne(userId)
                .orElseThrow(() -> new EntityNotFoundException("User negăsit."));

        event.addObserver(user); // Adaugare in lista (Subject logic)
        eventRepository.save(event); // Salvare in tabela M:N
    }

    @Override
    public String finishEvent(Long eventId) {
        Event event = eventRepository.findOne(eventId)
                .map(this::reconstructSubscribers)
                .orElseThrow(() -> new EntityNotFoundException("Eveniment negasit."));

        if (event.isFinished()) {
            throw new ServiceException("Cursa este deja finalizată.");
        }

        // Populam participantii actuali inainte de start
        if (event instanceof RaceEvent raceEvent) {
            List<Duck> currentDucks = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                    .filter(u -> u instanceof Duck).map(u -> (Duck) u).collect(Collectors.toList());
            raceEvent.setEligibleParticipants(currentDucks);
        }

        // Executa logica de calcul clasament si NOTIFICA toti abonatii (notifyObservers)
        String result = event.finishEvent();

        notifyObservers(result);
        event.setFinished(true);
        eventRepository.save(event);

        return result;
    }

    @Override
    public void addEvent(Event event) {
        eventValidator.validate(event);
        eventRepository.save(event);
    }

    @Override
    public Iterable<Event> findAllEvents() {
        // Luam toate evenimentele si pentru fiecare aplicam logica de reconstructie a abonatilor
        return StreamSupport.stream(eventRepository.findAll().spliterator(), false)
                .map(this::reconstructSubscribers) // Populeaza lista subscribers din tabela M:N
                .collect(Collectors.toList());
    }

    /**
     * Oprirea corecta a thread-urilor la inchiderea aplicatiei.
     */
    public void shutdown() {
        executorService.shutdown();
    }



    /**
     * Helper pentru generarea textului de clasament.
     */
    @Override
    public String getEventRanking(Long eventId) {
        // 1. Gasim evenimentul si reconstruim abonatii
        Event event = eventRepository.findOne(eventId)
                .map(this::reconstructSubscribers)
                .orElseThrow(() -> new EntityNotFoundException("Evenimentul nu exista."));

        // 2. Daca este o cursa, aplicam logica specifica de sortare
        if (event instanceof RaceEvent raceEvent) {
            // Obținem toti participantii inotatori eligibili
            List<Duck> swimmingDucks = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                    .filter(u -> u instanceof Duck && u instanceof Inotator)
                    .map(u -> (Duck) u)
                    .sorted(Comparator.comparingDouble(Duck::getRezistenta).reversed()) // Sortare dupa rezistenta
                    .collect(Collectors.toList());

            List<Double> distances = raceEvent.getBeaconDistances();
            int m = Math.min(swimmingDucks.size(), distances.size());

            StringBuilder sb = new StringBuilder();
            sb.append("--- CLASAMENT FINAL CURSA ---\n");

            double maxTime = 0;
            for (int i = 0; i < m; i++) {
                Duck d = swimmingDucks.get(i);
                double dist = distances.get(i);
                double time = (d.getViteza() > 0) ? (2.0 * dist / d.getViteza()) : 0; // Calcul timp dus-întors

                if(time > maxTime) maxTime = time;

                sb.append(String.format("%d. %s - Timp: %.3f s (Rezistența: %.1f)\n",
                        i + 1, d.getUsername(), time, d.getRezistenta()));
            }
            sb.append(String.format("\nDurata Totala Eveniment: %.3f s", maxTime));
            return sb.toString();
        }

        return "Eveniment finalizat: " + event.getName();
    }


    @Override
    public List<String> getFinishedResultsForUser(Long userId) {
        // 1. Apelăm repository-ul pentru a obține doar evenimentele terminate la care user-ul a participat
        // Această metodă folosește filtrarea eficientă pe care am implementat-o în EventFileRepository
        List<Event> finishedEvents = eventRepository.findFinishedEventsByUserId(userId);

        // 2. Transformăm lista de obiecte Event într-o Listă de String-uri (mesaje de clasament)
        return finishedEvents.stream()
                .map(event -> {
                    // Generăm clasamentul detaliat folosind logica de calcul din RaceEvent
                    String ranking = getEventRanking(event.getId());
                    return "Eveniment: " + event.getName() + "\n" + ranking;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteEvent(Long id) {
        // 1. Apelam repository-ul pentru stergere
        Optional<Event> deletedEvent = eventRepository.delete(id);

        // 2. Verificam daca evenimentul a existat si a fost sters cu succes
        if (deletedEvent.isPresent()) {
            // Notificam observatorii (UI) ca datele s-au schimbat
            // Trimitem un mesaj specific sau pur si simplu un semnal de refresh
            notifyObservers("EVENT_DELETED");
        } else {
            throw new EntityNotFoundException("Evenimentul cu ID-ul " + id + " nu a putut fi gasit pentru stergere.");
        }
    }
}