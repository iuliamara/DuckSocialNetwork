package com.ubb.domain;

/**
 * Interfata generica pentru un Subiect (Subject) in Pattern-ul Observer.
 * Define contractul pentru gestionarea si notificarea Observer-ilor (abonatilor).
 *
 * @param <E> Tipul de date al mesajului trimis catre Observeri.
 */
public interface Subject<E> {
    /**
     * Aboneaza un Observer la acest Subiect.
     * @param observer Observer-ul de adaugat.
     */
    void addObserver(Observer<E> observer);

    /**
     * Dezaboneaza un Observer de la acest Subiect.
     * @param observer Observer-ul de eliminat.
     */
    void removeObserver(Observer<E> observer);

    /**
     * Notifica toti Observer-ii abonati cu o anumita data/mesaj.
     * @param eventData Datele/Mesajul de trimis.
     */
    void notifyObservers(E eventData);
}