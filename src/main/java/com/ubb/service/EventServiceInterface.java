package com.ubb.service;

import com.ubb.domain.Event;
import com.ubb.domain.Subject;
import com.ubb.service.exceptions.EntityNotFoundException;
import com.ubb.service.exceptions.ServiceException;

import java.util.List;
import java.util.concurrent.CompletableFuture; // Import obligatoriu

/**
 * Interfata care defineste contractul pentru serviciile legate de entitatea Event.
 * Include metode pentru operatii asincrone conform cerintei Lab 10.
 */
public interface EventServiceInterface extends Subject<Object> {

    /**
     * Creeaza un RaceEvent nou.
     */
    void createRaceEvent(String name, int numParticipants, List<Double> beaconDistances);

    /**
     * Adauga un eveniment nou in sistem.
     */
    void addEvent(Event event);

    /**
     * Aboneaza un utilizator la un eveniment specific (M:N).
     */
    void subscribe(Long eventId, Long userId);

    /**
     * Finalizeaza un eveniment (Sincron).
     */
    String finishEvent(Long eventId);

    /**
     * NOU (LAB 10): Finalizeaza un eveniment in mod ASINCRON.
     * @param eventId ID-ul evenimentului de finalizat.
     * @return Un CompletableFuture care va contine rezultatul (clasamentul).
     */
    CompletableFuture<String> finishEventAsync(Long eventId);

    /**
     * Returneaza toate evenimentele din sistem.
     */
    Iterable<Event> findAllEvents();

    /**
     * Oprirea corecta a thread-urilor la inchiderea aplicatiei.
     */
    void shutdown();

    String getEventRanking(Long eventId);

    List<String> getFinishedResultsForUser(Long userId);

    void deleteEvent(Long id);
}