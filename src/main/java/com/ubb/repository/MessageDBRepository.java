package com.ubb.repository;

import com.ubb.domain.Message;
import com.ubb.domain.Persoana;
import com.ubb.domain.User;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.exceptions.RepositoryException;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MessageDBRepository implements MessageRepositoryInterface {

    private final String url;
    private final String username;
    private final String password;
    private final Validator<Message> validator;
    private final UserRepositoryInterface userRepository;

    public MessageDBRepository(String url, String username, String password, Validator<Message> validator, UserRepositoryInterface userRepository) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.validator = validator;
        this.userRepository = userRepository;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * CORECTAT: Extrage Message folosind constructorul cu isRead.
     */
    private Message extractMessage(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long senderId = rs.getLong("from_user_id");
        String content = rs.getString("text_mesaj").trim();
        LocalDateTime dateSent = rs.getTimestamp("data_mesaj").toLocalDateTime();
        Long replyToId = rs.getObject("reply_to_id") != null ? rs.getLong("reply_to_id") : null;
        boolean isRead = rs.getBoolean("is_read");

        User sender = userRepository.findOne(senderId)
                .orElseThrow(() -> new RepositoryException("Sender inexistent cu ID: " + senderId));

        List<User> recipients = findRecipients(id);

        Message replyToMessage = null;
        if (replyToId != null) {
            replyToMessage = findSimpleMessageDetails(replyToId);
        }

        // Instanțiere directă cu starea corectă
        Message message = new Message(sender, recipients, content, dateSent, replyToMessage, isRead);
        message.setId(id);

        return message;
    }

    /**
     * CORECTAT: Folosește constructorul complet și aici.
     */
    private Message findSimpleMessageDetails(Long id) {
        String sql = "SELECT id, from_user_id, text_mesaj, data_mesaj, is_read FROM messages WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long senderId = rs.getLong("from_user_id");
                    String content = rs.getString("text_mesaj").trim();
                    LocalDateTime dateSent = rs.getTimestamp("data_mesaj").toLocalDateTime();
                    boolean isRead = rs.getBoolean("is_read");

                    User sender = userRepository.findOne(senderId)
                            .orElseThrow(() -> new RepositoryException("Sender inexistent cu ID: " + senderId));

                    // Folosim constructorul complet pentru 'shell' (lista de destinatari goală)
                    Message simpleMessage = new Message(sender, new ArrayList<>(), content, dateSent, null, isRead);
                    simpleMessage.setId(id);
                    return simpleMessage;
                }
            }
        } catch (SQLException | RepositoryException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<User> findRecipients(Long messageId) {
        List<User> recipients = new ArrayList<>();
        String sql = "SELECT recipient_user_id FROM recipients WHERE message_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    userRepository.findOne(rs.getLong("recipient_user_id")).ifPresent(recipients::add);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return recipients;
    }

    @Override
    public List<Message> getConversation(Long idUser1, Long idUser2) {
        List<Message> conversation = new ArrayList<>();
        // SQL optimizat: JOIN pentru expeditor și LEFT JOIN pentru mesajul de reply
        String sql = """
        SELECT m.id, m.from_user_id, m.text_mesaj, m.data_mesaj, m.reply_to_id, m.is_read, 
               u.username AS sender_username,
               parent.text_mesaj AS reply_text
        FROM messages m
        JOIN users u ON m.from_user_id = u.id
        JOIN recipients r ON m.id = r.message_id
        LEFT JOIN messages parent ON m.reply_to_id = parent.id
        WHERE (m.from_user_id = ? AND r.recipient_user_id = ?) 
           OR (m.from_user_id = ? AND r.recipient_user_id = ?)
        ORDER BY m.data_mesaj ASC
        """;
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, idUser1); ps.setLong(2, idUser2);
            ps.setLong(3, idUser2); ps.setLong(4, idUser1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    conversation.add(extractMessageFast(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conversation;
    }

    /**
     * Extracție rapidă fără interogări SQL suplimentare în buclă.
     */
    private Message extractMessageFast(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long senderId = rs.getLong("from_user_id");
        String senderName = rs.getString("sender_username");
        String content = rs.getString("text_mesaj").trim();
        LocalDateTime dateSent = rs.getTimestamp("data_mesaj").toLocalDateTime();
        boolean isRead = rs.getBoolean("is_read");

        // Extragem informațiile de reply din JOIN
        Long replyToId = rs.getObject("reply_to_id") != null ? rs.getLong("reply_to_id") : null;
        String replyText = rs.getString("reply_text");

        // REPARAT: Folosim Persoana (clasă concretă) pentru a evita eroarea 'User is abstract'
        // Trimitem date dummy pentru câmpurile neesențiale în chat pentru a menține viteza.
        Persoana sender = new Persoana(
                senderName,      // username (REAL)
                "",              // email
                "",              // passwordHash
                "",              // nume
                "",              // prenume
                java.time.LocalDate.now(), // dataNasterii
                "",              // ocupatie
                0                // nivelEmpatie
        );
        sender.setId(senderId);

        Message replyMessage = null;
        if (replyToId != null) {
            // Creăm un sender minimal și pentru reply pentru a evita NullPointerException în UI
            User replySender = new Persoana("User", "", "", "", "", java.time.LocalDate.now(), "", 0);

            replyMessage = new Message(
                    replySender,
                    new ArrayList<>(),
                    replyText != null ? replyText : "Mesaj indisponibil",
                    null,
                    null,
                    true
            );
            replyMessage.setId(replyToId);
        }

        // Creăm mesajul final cu toate legăturile populate
        Message message = new Message(sender, new ArrayList<>(), content, dateSent, replyMessage, isRead);
        message.setId(id);
        return message;
    }

    @Override
    public Optional<Message> save(Message entity) {
        if (entity == null) throw new IllegalArgumentException("Entitatea nu poate fi nula.");
        validator.validate(entity);

        String insertMsgSql = "INSERT INTO messages (from_user_id, text_mesaj, reply_to_id, data_mesaj, is_read) VALUES (?, ?, ?, ?, ?) RETURNING id";
        String insertRecipientSql = "INSERT INTO recipients (message_id, recipient_user_id) VALUES (?, ?)";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement msgPs = connection.prepareStatement(insertMsgSql)) {
                msgPs.setLong(1, entity.getFrom().getId());
                msgPs.setString(2, entity.getMessage());
                if (entity.getReply() != null) msgPs.setLong(3, entity.getReply().getId());
                else msgPs.setNull(3, Types.BIGINT);
                msgPs.setTimestamp(4, Timestamp.valueOf(entity.getData()));
                msgPs.setBoolean(5, entity.isRead());

                try (ResultSet rs = msgPs.executeQuery()) {
                    if (rs.next()) entity.setId(rs.getLong(1));
                }
            }
            try (PreparedStatement recPs = connection.prepareStatement(insertRecipientSql)) {
                for (User recipient : entity.getTo()) {
                    recPs.setLong(1, entity.getId());
                    recPs.setLong(2, recipient.getId());
                    recPs.addBatch();
                }
                recPs.executeBatch();
            }
            connection.commit();
            return Optional.of(entity);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void update(Message entity) {
        String sql = "UPDATE messages SET is_read = ? WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, entity.isRead());
            ps.setLong(2, entity.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Iterable<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, from_user_id, text_mesaj, data_mesaj, reply_to_id, is_read FROM messages";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                messages.add(extractMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public Optional<Message> findOne(Long id) {
        String sql = "SELECT id, from_user_id, text_mesaj, data_mesaj, reply_to_id, is_read FROM messages WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(extractMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public long size() {
        String sql = "SELECT COUNT(*) FROM messages";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public Optional<Message> delete(Long id) { return Optional.empty(); }

    @Override
    public long countUnreadMessages(Long userId) {
        String sql = "SELECT COUNT(*) FROM messages m " +
                "JOIN recipients r ON m.id = r.message_id " +
                "WHERE r.recipient_user_id = ? AND m.is_read = false";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public long countUnreadMessagesFrom(Long senderId, Long recipientId) {
        String sql = """
        SELECT COUNT(*) 
        FROM messages m
        JOIN recipients r ON m.id = r.message_id
        WHERE m.from_user_id = ? 
          AND r.recipient_user_id = ? 
          AND m.is_read = false
        """;
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, senderId);
            ps.setLong(2, recipientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}