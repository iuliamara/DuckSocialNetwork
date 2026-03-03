package com.ubb.service;

import com.ubb.domain.Message;
import com.ubb.domain.Subject;
import com.ubb.service.exceptions.ServiceException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interfata care defineste contractul pentru serviciile legate de entitatea Message.
 * Gestioneaza logica de business pentru trimiterea si vizualizarea mesajelor.
 */
public interface MessageServiceInterface extends Subject<Message> {

    /**
     * Trimite un mesaj de la un utilizator catre o lista de destinatari.
     * @param senderId ID-ul utilizatorului care trimite.
     * @param recipientIds Lista ID-urilor destinatarilor.
     * @param content Continutul mesajului.
     * @param replyTo Mesajul la care se raspunde (poate fi null).
     * @return Mesajul salvat (cu ID-ul generat si data setata).
     * @throws ServiceException Daca un utilizator (sender/recipient) nu exista sau daca validarea esueaza.
     */
    Message sendMessage(Long senderId, List<Long> recipientIds, String content, Message replyTo) throws ServiceException;

    /**
     * Obtine conversatia istorica (mesajele trimise in ambele sensuri) intre doi utilizatori.
     * Mesajele sunt sortate cronologic.
     * @param idUser1 ID-ul primului utilizator.
     * @param idUser2 ID-ul celui de-al doilea utilizator.
     * @return Lista de mesaje (trimise sau primite de unul dintre ei).
     */
    List<Message> getConversation(Long idUser1, Long idUser2);

    /**
     * Calculează numărul de mesaje necitite pentru un utilizator.
     */
    long getUnreadCount(Long userId);

    /**
     * Marchează toate mesajele primite de la un prieten specific ca fiind citite.
     */
    void markConversationAsRead(Long currentUserId, Long friendId);

    long getUnreadCountFrom(Long senderId, Long recipientId) ;

    boolean hasUnreadMessagesFrom(Long senderId, Long recipientId);

    CompletableFuture<Message> sendMessageAsync(Long senderId, List<Long> recipientIds, String content, Message replyTo);
}