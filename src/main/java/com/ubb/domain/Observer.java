package com.ubb.domain;

/**
 * Interfata generica pentru un Observer (Abonat) in Pattern-ul Observer.
 * Define contractul pentru a primi si a reactiona la notificari de la un Subiect.
 *
 * @param <E> Tipul de date al mesajului primit in notificare (String, Message, etc.).
 */
public interface Observer<E> {
    /**
     * Metoda apelata de Subject/Observable cand starea sa se schimba.
     * Aceasta este actiunea de reactie a observatorului.
     * @param subject Subiectul (Observable) care trimite notificarea.
     * @param eventData Datele/Mesajul specific trimis.
     */
    void update(Subject<E> subject, E eventData);
}