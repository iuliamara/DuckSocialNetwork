package com.ubb.repository;

import com.ubb.domain.FriendshipRequest;
import com.ubb.domain.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface FriendshipRequestRepositoryInterface extends Repository<Long, FriendshipRequest>{
    /**
     * Gaseste cererile primite (toUser) care au un status din lista specificata.
     * @param toUserId ID-ul utilizatorului destinatar.
     * @param statuses Lista de statusuri dorite (ex: PENDING, APPROVED).
     * @return Lista de cereri.
     */
    List<FriendshipRequest> findRequestsByRecipientAndStatuses(Long toUserId, List<RequestStatus> statuses);
    /**
     * Gaseste o cerere existenta intre doi utilizatori, indiferent de status.
     * Utila pentru a verifica daca o cerere (PENDING) exista deja.
     * @param fromUserId ID-ul expeditorului.
     * @param toUserId ID-ul destinatarului.
     * @return Optional<FriendshipRequest>.
     */
    Optional<FriendshipRequest> findRequestBySenderAndRecipient(Long fromUserId, Long toUserId);

    List<FriendshipRequest> findRequestsBySender(Long senderId, RequestStatus status);

    List<FriendshipRequest> findRequestsByReceiver(Long fromUserId, RequestStatus status);

    int getUnreadNotificationsCount(Long userId);
}
