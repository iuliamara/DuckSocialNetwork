package com.ubb.service;

import com.ubb.domain.*;
import com.ubb.domain.exceptions.ValidationException;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.FriendshipRepositoryInterface;
import com.ubb.repository.FriendshipRequestRepositoryInterface;
import com.ubb.repository.UserRepositoryInterface;
import com.ubb.repository.exceptions.RepositoryException;
import com.ubb.service.exceptions.EntityNotFoundException;
import com.ubb.service.exceptions.ServiceException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FriendshipRequestService implements FriendshipRequestServiceInterface{
    private final FriendshipRequestRepositoryInterface requestRepository;
    private final UserRepositoryInterface userRepository; // Reprezinta UserDBRepository
    private final FriendshipRepositoryInterface friendshipRepository; // Reprezinta FriendshipDBRepository
    private final Validator<FriendshipRequest> requestValidator;

    // Lista de observatori abonati (Ex: NotificationController)
    private final List<Observer<FriendshipRequest>> observers = new ArrayList<>();

    public FriendshipRequestService(FriendshipRequestRepositoryInterface requestRepository, UserRepositoryInterface userRepository, FriendshipRepositoryInterface friendshipRepository, Validator<FriendshipRequest> requestValidator) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.requestValidator = requestValidator;
    }

    // Implementarea metodelor Subject
    @Override
    public void addObserver(Observer<FriendshipRequest> observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(Observer<FriendshipRequest> observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(FriendshipRequest request) {
        System.out.println("Service: Notificare trimisa! Cerere ID: " + request.getId());
        for (Observer<FriendshipRequest> observer : observers) {
            observer.update(this, request);
        }
    }

    /**
     * Helper: Cauta un utilizator sau arunca exceptie.
     */
    private User findUserOrThrow(Long userId, String role) throws ServiceException {
        return userRepository.findOne(userId)
                .orElseThrow(() -> new EntityNotFoundException(role + " cu ID " + userId + " nu exista."));
    }

    // --- 1. TRIMITE CERERE (PENDING) ---

    @Override
    public FriendshipRequest sendRequest(Long fromUserId, Long toUserId) throws ServiceException {
        // 1. Validare existență utilizatori
        User sender = findUserOrThrow(fromUserId, "Expeditor");
        User recipient = findUserOrThrow(toUserId, "Destinatar");

        // 2. Validare logică: Expeditorul și destinatarul trebuie să fie diferiți
        if (fromUserId.equals(toUserId)) {
            throw new ServiceException("Nu puteti trimite o cerere de prietenie catre dumneavoastra insiva.");
        }

        // 3. Validare logică: Verificăm dacă sunt deja prieteni în tabelul de prietenii
        if (friendshipRepository.findOne(new Tuple<>(fromUserId, toUserId)).isPresent() ||
                friendshipRepository.findOne(new Tuple<>(toUserId, fromUserId)).isPresent()) {
            throw new ServiceException("Relatia de prietenie intre acesti utilizatori exista deja.");
        }

        // 4. Verificare cereri existente de la A (eu) la B (el)
        Optional<FriendshipRequest> existingRequestOpt = requestRepository.findRequestBySenderAndRecipient(fromUserId, toUserId);

        if (existingRequestOpt.isPresent()) {
            FriendshipRequest existingRequest = existingRequestOpt.get();

            // CAZ A: Cerere deja în așteptare -> Nu putem trimite alta
            if (existingRequest.getStatus() == RequestStatus.PENDING) {
                throw new ServiceException("Exista deja o cerere de prietenie trimisa de dvs. catre acest utilizator.");
            }

            // CAZ B & C: Cererea a fost RESPINSĂ (REJECTED) sau APROBATĂ (APPROVED) anterior
            // Deoarece am trecut de pasul 3, știm că prietenia nu mai există fizic.
            // Resetăm cererea existentă la PENDING pentru a permite re-trimiterea.
            if (existingRequest.getStatus() == RequestStatus.REJECTED ||
                    existingRequest.getStatus() == RequestStatus.APPROVED) {

                existingRequest.setStatus(RequestStatus.PENDING);
                existingRequest.setDateSent(LocalDateTime.now());
                existingRequest.setViewedByRecipient(false); // Pentru ca destinatarul să vadă Bold și butonul roșu

                try {
                    // Actualizăm rândul existent în DB
                    requestRepository.save(existingRequest);

                    // Notificăm observatorii (MainController va pune badge-ul roșu la destinatar)
                    notifyObservers(existingRequest);

                    return existingRequest;
                } catch (RepositoryException e) {
                    throw new ServiceException("Eroare la reactivarea cererii: " + e.getMessage());
                }
            }
        }

        // 5. Verificare cereri inverse: B (el) ți-a trimis ție și e PENDING
        Optional<FriendshipRequest> inverseRequestOpt = requestRepository.findRequestBySenderAndRecipient(toUserId, fromUserId);
        if (inverseRequestOpt.isPresent()) {
            if (inverseRequestOpt.get().getStatus() == RequestStatus.PENDING) {
                throw new ServiceException("Exista deja o cerere in asteptare de la acest utilizator. Verificati sectiunea Notificari.");
            }
        }

        // 6. Creare și Salvare cerere complet nouă (dacă nu a existat nimic anterior)
        FriendshipRequest newRequest = new FriendshipRequest(sender, recipient);

        try {
            requestValidator.validate(newRequest);

            Optional<FriendshipRequest> result = requestRepository.save(newRequest);

            if (result.isPresent()) {
                throw new ServiceException("Salvarea cererii a esuat.");
            }

            // Notificăm observatorii pentru badge-ul roșu în timp real
            notifyObservers(newRequest);

            return newRequest;

        } catch (RepositoryException e) {
            throw new ServiceException("Eroare la salvarea in baza de date: " + e.getMessage());
        } catch (ValidationException e) {
            throw new ServiceException("Validarea cererii a esuat: " + e.getMessage());
        }
    }

    // --- 2. ACCEPTA CERERE (APPROVED) ---

    @Override
    public Friendship acceptRequest(Long requestId) throws ServiceException {
        // 1. Gaseste cererea si valideaza starea
        FriendshipRequest request = requestRepository.findOne(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Cererea cu ID " + requestId + " nu exista."));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ServiceException("Cererea nu este in starea PENDING (status curent: " + request.getStatus() + ").");
        }

        Long idUser1 = request.getFromUser().getId();
        Long idUser2 = request.getToUser().getId();

        // 2. Salveaza noua prietenie in FriendshipRepository
        Friendship newFriendship = new Friendship(idUser1, idUser2);
        Optional<Friendship> friendshipResult = friendshipRepository.save(newFriendship);

        if (friendshipResult.isPresent()) {
            throw new ServiceException("Eroare la crearea relatiei de prietenie in baza de date.");
        }

        // 3. Marcheaza cererea ca APPROVED in RequestRepository (face UPDATE)
        request.setStatus(RequestStatus.APPROVED);
        request.setViewedByRecipient(true);
        requestRepository.save(request); // Save face UPDATE deoarece ID-ul exista

        notifyObservers(request);

        return newFriendship;
    }

    // --- 3. RESPINGE CERERE (REJECTED) ---

    @Override
    public FriendshipRequest rejectRequest(Long requestId) throws ServiceException {
        // 1. Gaseste cererea si valideaza starea
        FriendshipRequest request = requestRepository.findOne(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Cererea cu ID " + requestId + " nu exista."));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ServiceException("Cererea nu este in starea PENDING (status curent: " + request.getStatus() + ").");
        }

        // 2. Marcheaza cererea ca REJECTED in RequestRepository (face UPDATE)
        request.setStatus(RequestStatus.REJECTED);
        request.setViewedByRecipient(true);
        requestRepository.save(request);

        notifyObservers(request);

        return request;
    }

    /**
     * Marcheaza o cerere ca vizualizata fara a notifica observatorii.
     */
    public void markRequestAsViewedSilent(Long requestId) throws ServiceException {
        FriendshipRequest request = requestRepository.findOne(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Cererea cu ID " + requestId + " nu exista."));

        if (request == null) {
            throw new ServiceException("Cererea nu a fost gasita.");
        }

        if (!request.isViewedByRecipient()) {
            request.setViewedByRecipient(true);

            // Salveaza doar schimbarea de stare in DB
            requestRepository.save(request);
        }
    }

    @Override
    public long getUnseenNotificationCount(Long userId) {
        // numara toate notificarile (PENDING/APPROVED) nevizualizate
        List<FriendshipRequest> notifications = getNotifications(userId);

        return notifications.stream()
                .filter(request -> !request.isViewedByRecipient())
                .count();
    }


    @Override
    public List<FriendshipRequest> getNotifications(Long userId) {

        // 1. Definirea statusurilor relevante pentru notificari (cele primite de utilizator)
        List<RequestStatus> notificationStatuses = List.of(
                RequestStatus.PENDING,
                RequestStatus.APPROVED,
                RequestStatus.REJECTED
        );

        // 2. Delegarea catre metoda optimizata din Repository
        // Metoda findRequestsByRecipientAndStatuses cauta cererile unde 'toUser' este userId
        try {
            return requestRepository.findRequestsByRecipientAndStatuses(userId, notificationStatuses);
        } catch (Exception e) {
            // Prindem exceptiile din Repository si le ridicam ca ServiceException
            throw new ServiceException("Eroare la extragerea notificarilor: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FriendshipRequest> getRequestsBySender(Long senderId, RequestStatus status) throws ServiceException {
        // 1. Validare Existenta User
        findUserOrThrow(senderId, "Expeditor"); // Reutilizam helper-ul existent

        try {
            // 2. Delegarea catre metoda optimizata din Repository
            // Repository-ul va gestiona logica SQL pentru status = NULL sau status specific
            return requestRepository.findRequestsBySender(senderId, status);
        } catch (RepositoryException e) {
            throw new ServiceException("Eroare la extragerea cererilor trimise: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FriendshipRequest> getRequestsByReceiver(Long senderId, RequestStatus status) throws ServiceException {
        // 1. Validare Existenta User
        findUserOrThrow(senderId, "Expeditor"); // Reutilizam helper-ul existent

        try {
            // 2. Delegarea catre metoda optimizata din Repository
            // Repository-ul va gestiona logica SQL pentru status = NULL sau status specific
            return requestRepository.findRequestsByReceiver(senderId, status);
        } catch (RepositoryException e) {
            throw new ServiceException("Eroare la extragerea cererilor trimise: " + e.getMessage(), e);
        }
    }

    /**
     * Marcheaza o singura cerere ca vizualizata si notifica observatorii.
     */
    @Override
    public void markAsViewed(Long requestId) throws ServiceException {
        FriendshipRequest request = requestRepository.findOne(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Cererea cu ID " + requestId + " nu exista."));

        if (!request.isViewedByRecipient()) {
            request.setViewedByRecipient(true);

            // 1. Salvare în baza de date (UPDATE)
            requestRepository.save(request);

            // 2. NOTIFICARE: Aceasta va declanșa update() în MainController și NotificationController
            notifyObservers(request);
        }
    }

    @Override
    public Optional<FriendshipRequest> getRelationStatus(Long id1, Long id2) {
        // Căutăm dacă eu i-am trimis lui
        Optional<FriendshipRequest> sent = requestRepository.findRequestBySenderAndRecipient(id1, id2);
        if (sent.isPresent()) return sent;

        // Sau dacă el mi-a trimis mie
        return requestRepository.findRequestBySenderAndRecipient(id2, id1);
    }
}
