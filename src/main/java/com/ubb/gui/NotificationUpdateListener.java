package com.ubb.gui;

/**
 * Interfata de callback utilizata pentru a notifica componentele de nivel superior
 * (de exemplu, MainController-ul care gestioneaza TabPane-ul) despre schimbarile
 * in starea de "nevazut" (unread count) a notificarilor.
 */
public interface NotificationUpdateListener {

    /**
     * Metoda apelata de NotificationController pentru a trimite numarul curent
     * de elemente nevazute.
     * * @param count Numarul total de elemente (FriendshipRequest) care nu au fost inca vizualizate.
     */
    void updateUnreadCount(long count);
}