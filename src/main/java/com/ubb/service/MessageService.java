package com.ubb.service;

import com.ubb.domain.Message;
import com.ubb.domain.Observer;
import com.ubb.domain.User;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.MessageRepositoryInterface;
import com.ubb.repository.UserRepositoryInterface;
import com.ubb.service.exceptions.ServiceException;
import com.ubb.service.exceptions.EntityNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MessageService implements MessageServiceInterface {

    private final List<Observer<Message>> observers = new ArrayList<>();
    private final MessageRepositoryInterface messageRepository;
    private final UserRepositoryInterface userRepository;
    private final Validator<Message> messageValidator;

    public MessageService(MessageRepositoryInterface messageRepository, UserRepositoryInterface userRepository, Validator<Message> messageValidator) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messageValidator = messageValidator;
    }

    @Override
    public void addObserver(Observer<Message> observer) {
        if (!observers.contains(observer)) observers.add(observer);
    }

    @Override
    public void removeObserver(Observer<Message> observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Message message) {
        String msgId = (message != null) ? message.getId().toString() : "REFRESH_ALL";
        System.out.println("Service: Notificare trimisa. Mesaj ID: " + msgId);
        observers.forEach(observer -> observer.update(this, message));
    }

    @Override
    public Message sendMessage(Long senderId, List<Long> recipientIds, String content, Message replyTo) throws ServiceException {
        User sender = userRepository.findOne(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Sender-ul cu ID " + senderId + " nu exista."));

        List<User> recipients = recipientIds.stream()
                .map(id -> userRepository.findOne(id)
                        .orElseThrow(() -> new EntityNotFoundException("Destinatarul cu ID " + id + " nu exista.")))
                .collect(Collectors.toList());

        if (recipients.isEmpty()) throw new ServiceException("Trebuie sa specificati cel putin un destinatar.");

        Message newMessage = new Message(sender, recipients, content, replyTo);
        messageValidator.validate(newMessage);

        Message savedMessage = messageRepository.save(newMessage)
                .orElseThrow(() -> new ServiceException("Salvarea mesajului a esuat in repository."));

        notifyObservers(savedMessage);
        return savedMessage;
    }

    @Override
    public List<Message> getConversation(Long idUser1, Long idUser2) {
        return messageRepository.getConversation(idUser1, idUser2);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return messageRepository.countUnreadMessages(userId);
    }

    @Override
    public void markConversationAsRead(Long currentUserId, Long friendId) {
        // Obținem conversația pentru a identifica mesajele necitite
        List<Message> conversation = messageRepository.getConversation(currentUserId, friendId);

        boolean changed = false;
        for (Message msg : conversation) {
            // Regula: Doar mesajele primite de la prieten sunt marcate ca citite
            if (msg.getFrom().getId().equals(friendId) && !msg.isRead()) {
                msg.setRead(true);
                messageRepository.update(msg);
                changed = true;
            }
        }

        if (changed) {
            notifyObservers(null);
        }
    }

    @Override
    public long getUnreadCountFrom(Long senderId, Long recipientId) {
        return messageRepository.countUnreadMessagesFrom(senderId, recipientId);
    }

    @Override
    public boolean hasUnreadMessagesFrom(Long senderId, Long recipientId) {
        return messageRepository.countUnreadMessagesFrom(senderId, recipientId) > 0;
    }

    public CompletableFuture<Message> sendMessageAsync(Long senderId, List<Long> recipientIds, String content, Message replyTo) {
        // Rulam tot blocul de logica pe un thread separat
        return CompletableFuture.supplyAsync(() -> {
            try {
                User sender = userRepository.findOne(senderId)
                        .orElseThrow(() -> new EntityNotFoundException("Sender-ul cu ID " + senderId + " nu exista."));

                List<User> recipients = recipientIds.stream()
                        .map(id -> userRepository.findOne(id)
                                .orElseThrow(() -> new EntityNotFoundException("Destinatarul cu ID " + id + " nu exista.")))
                        .collect(Collectors.toList());

                if (recipients.isEmpty()) throw new ServiceException("Trebuie sa specificat cel putin un destinatar.");

                Message newMessage = new Message(sender, recipients, content, replyTo);
                messageValidator.validate(newMessage);

                Message savedMessage = messageRepository.save(newMessage)
                        .orElseThrow(() -> new ServiceException("Salvarea mesajului a esuat in repository."));

                // Notificam observatorii tot din thread-ul asincron
                notifyObservers(savedMessage);

                return savedMessage;
            } catch (Exception e) {
                // Aruncam o exceptie runtime pentru a fi prinsa de in controller
                throw new RuntimeException(e.getMessage());
            }
        });
    }
}