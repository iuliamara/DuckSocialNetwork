package com.ubb.domain;

/**
 * Clasa concreta care reprezinta un Card ce poate contine doar Rate Inotatoare (SwimmingDuck).
 * Extinde Card<SwimmingDuck> si mosteneste logica de performanta si management al membrilor.
 */
public class SwimmingCard extends Card<SwimmingDuck> {

    /**
     * Constructor pentru initializarea SwimmingCard.
     * @param numeCard Numele cardului.
     */
    public SwimmingCard(String numeCard) {
        // Apelam constructorul clasei de baza (Card<SwimmingDuck>)
        super(numeCard);
    }

    // Metodele toString(), getPerformantaMedie(), addMembru() etc. sunt mostenite din Card.
}