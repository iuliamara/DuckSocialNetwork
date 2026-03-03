package com.ubb.repository;
import com.ubb.domain.Message;
import com.ubb.domain.User;
import java.sql.SQLException;
import java.util.List;

/**
 * Interfata specifica pentru Repository-ul de Mesaje.
 * Extinde interfața generica Repository pentru a mosteni contractul CRUD.
 * Adauga metode specifice pentru gestionarea relatiilor complexe (M:N si filtrare).
 */
public interface MessageRepositoryInterface extends Repository<Long, Message> {

    /**
     * Obtine lista de destinatari (User) asociati unui mesaj specificat.
     * Aceasta gestioneaza relatia multi-to-multi (M:N) dintre Mesaj si User.
     * @param messageId ID-ul mesajului.
     * @return Lista de obiecte User care sunt destinatari.
     * @throws SQLException Daca apare o eroare la interogarea bazei de date.
     */
    List<User> findRecipients(Long messageId) throws SQLException;

    /**
     * Obtine conversatia (toate mesajele trimise in ambele sensuri) dintre doi utilizatori.
     * Mesajele ar trebui returnate sortate cronologic.
     * @param user1 ID-ul primului utilizator.
     * @param user2 ID-ul celui de-al doilea utilizator.
     * @return Lista de Mesaje care formeaza conversatia dintre cei doi.
     */
    List<Message> getConversation(Long user1, Long user2);

    void update(Message entity);

    long countUnreadMessages(Long userId);

    long countUnreadMessagesFrom(Long senderId, Long recipientId);
}