package com.ubb.service;

import com.ubb.domain.Friendship;
import com.ubb.domain.FriendshipRequest;
import com.ubb.domain.RequestStatus;
import com.ubb.domain.Subject;
import com.ubb.service.exceptions.ServiceException;

import java.util.List;
import java.util.Optional;

public interface FriendshipRequestServiceInterface extends Subject<FriendshipRequest> {
    /**
     * Trimite o cerere de prietenie de la un utilizator la altul.
     * @param fromUserId ID-ul utilizatorului care trimite.
     * @param toUserId ID-ul utilizatorului care primeste.
     * @return Cererea de prietenie salvata.
     * @throws ServiceException Daca utilizatorii nu exista, prietenia exista deja sau o cerere pending inversa exista.
     */
    FriendshipRequest sendRequest(Long fromUserId, Long toUserId) throws ServiceException;

    /**
     * Accepta o cerere de prietenie in asteptare.
     * Aceasta actiune salveaza relatia in FriendshipRepository si actualizeaza statusul cererii la APPROVED.
     * @param requestId ID-ul cererii de prietenie.
     * @return Prietenia nou creata.
     * @throws ServiceException Daca cererea nu exista, nu este PENDING sau utilizatorii nu exista.
     */
    Friendship acceptRequest(Long requestId) throws ServiceException;

    /**
     * Respinge o cerere de prietenie in asteptare.
     * Actualizeaza statusul cererii la REJECTED.
     * @param requestId ID-ul cererii de prietenie.
     * @return Cererea respinsa.
     * @throws ServiceException Daca cererea nu exista sau nu este PENDING.
     */
    FriendshipRequest rejectRequest(Long requestId) throws ServiceException;

    /**
     * Marcheaza o singura notificare primita ca fiind vizualizata (isViewedByRecipient = true).
     * @param requestId ID-ul cererii de prietenie.
     */
    void markRequestAsViewedSilent(Long requestId) throws ServiceException;

    /**
     * Returneaza numarul de notificari noi (nevizualizate).
     * (Ramane utila pentru bulina rosie/culoarea tab-ului)
     * @param userId ID-ul utilizatorului autentificat.
     * @return Numarul de notificari unde isViewedByRecipient este FALSE.
     */
    long getUnseenNotificationCount(Long userId);

    /**
     * Obtine cererile de prietenie primite de un anumit utilizator,
     * inclusiv cele in asteptare (PENDING) si cele recent acceptate (APPROVED).
     *
     * @param userId ID-ul utilizatorului autentificat.
     * @return Lista de cereri care servesc ca notificari.
     */
    List<FriendshipRequest> getNotifications(Long userId);

    List<FriendshipRequest> getRequestsBySender(Long senderId, RequestStatus status) throws ServiceException;

    List<FriendshipRequest> getRequestsByReceiver(Long senderId, RequestStatus status) throws ServiceException;

    void markAsViewed(Long requestId) throws ServiceException;

    Optional<FriendshipRequest> getRelationStatus(Long fromUserId, Long toUserId);
}
