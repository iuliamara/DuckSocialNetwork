package com.ubb.domain;

/**
 * Clasa concreta care reprezinta un Card ce poate contine doar Rate Zburatoare (FlyingDuck).
 * Extinde Card<FlyingDuck> si mosteneste logica de performanta si management al membrilor.
 */
public class FlyingCard extends Card<FlyingDuck> {

    /**
     * Constructor pentru initializarea FlyingCard.
     * @param numeCard Numele cardului.
     */
    public FlyingCard(String numeCard) {
        // Apelam constructorul clasei de baza (Card<FlyingDuck>)
        super(numeCard);
    }

    // Metodele toString(), getPerformantaMedie(), addMembru() etc. sunt mostenite din Card.
}