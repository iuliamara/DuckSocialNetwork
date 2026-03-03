package com.ubb.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Clasa abstracta Event (Subiectul/ Observable Pattern).
 * Extinde Entity si implementeaza Subject, gestionand abonatii si logica de notificare.
 */
public abstract class Event extends Entity<Long> implements Subject<String>{
    private final String name;
    // Lista de observatori foloseste tipul Observer<String>
    private final List<Observer<String>> subscribers = new ArrayList<>();

    private boolean isFinished;

    /**
     * Constructor pentru un eveniment nou.
     * @param name Numele evenimentului.
     */
    public Event(String name){
        this.name = name;
        this.isFinished = false; // Evenimentul nou este neterminat
    }

    // --- Specificatii: Getters & Setters ---

    /**
     * Seteaza starea de finalizare a evenimentului.
     * @param finished True daca evenimentul s-a terminat, false altfel.
     */
    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    /**
     * Verifica daca evenimentul s-a terminat.
     * @return True daca este terminat.
     */
    public boolean isFinished() {
        return isFinished;
    }

    /**
     * Returneaza numele evenimentului.
     * @return Numele.
     */
    public String getName(){
        return name;
    }

    /**
     * Returneaza lista de observatori (Useri) abonati la eveniment.
     * Metoda necesara pentru serializare/persistenta in Repository.
     * @return Lista de observatori.
     */
    public List<Observer<String>> getSubscribers() {
        return subscribers;
    }

    /**
     * Goleate lista abonatilor. Folosita la inceputul reconstructiei.
     */
    public void clearSubscribers() {
        this.subscribers.clear();
    }

    // --- Specificatii: Metode de Reconstructie/Populare ---

    /**
     * Seteaza lista completa de abonati (folosit doar de EventService la reconstructie).
     * @param observers Lista de Observeri de adaugat.
     */
    public void setSubscribers(List<Observer<String>> observers) {
        this.subscribers.clear();
        this.subscribers.addAll(observers);
    }

    // --- Specificatii: Observer Pattern Logic (Stream-uri) ---

    /**
     * Aboneaza un Observer la eveniment.
     * @param observer Observer-ul de adaugat.
     */
    @Override
    public void addObserver(Observer<String> observer){
        if(!subscribers.contains(observer)){
            subscribers.add(observer);
        }
    }

    /**
     * Dezaboneaza un Observer.
     * @param observer Observer-ul de eliminat.
     */
    @Override
    public void removeObserver(Observer<String> observer){
        subscribers.remove(observer);
    }

    /**
     * Metoda helper pentru a mentine compatibilitatea (notifica toti abonatii).
     * @param message Mesajul de trimis.
     */
    public void notifySubscribers(String message){
        notifyObservers(message);
    }

    /**
     * Notifica toti abonatii folosind operatii pe stream-uri.
     * @param message Mesajul de trimis.
     */
    @Override
    public void notifyObservers(String message){
        // OPERATIE PE STREAM-URI: Inlocuieste bucla for
        subscribers
                .forEach(observer -> observer.update(this, message));
    }


    // --- Specificatii: Metode Abstracte ---

    /**
     * Metoda abstracta ce contine logica specifica de finalizare a evenimentului.
     * @return Mesajul de finalizare care va fi trimis abonatilor.
     */
    public abstract String finishEvent();
}