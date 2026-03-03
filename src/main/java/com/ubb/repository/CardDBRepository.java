package com.ubb.repository;

import com.ubb.domain.Card;
import com.ubb.domain.Duck;
import com.ubb.domain.SwimmingCard;
import com.ubb.domain.FlyingCard;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.exceptions.RepositoryException;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repository care gestioneaza persistenta entitatilor de tip Card in baza de date PostgreSQL.
 * Implementeaza logica de persistenta M:N pentru membrii unui card (card_members).
 */
public class CardDBRepository implements CardRepositoryInterface {

    private final String url;
    private final String username;
    private final String password;
    private final Validator<Card<? extends Duck>> validator;

    /**
     * Constructor pentru initializarea conexiunii la baza de date.
     */
    public CardDBRepository(String url, String username, String password, Validator<Card<? extends Duck>> validator) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.validator = validator;
    }

    /**
     * Obtine o noua conexiune la baza de date.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    // --- METODE HELPER PENTRU M:N ---

    /**
     * Sterge toate inregistrarile vechi din tabela de asociere (card_members).
     */
    private void deleteMembers(Connection conn, Long cardId) throws SQLException {
        String sql = "DELETE FROM card_members WHERE card_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardId);
            ps.executeUpdate();
        }
    }

    /**
     * Insereaza ID-urile membrilor noi in tabela de asociere folosind Batch.
     */
    private void insertMembers(Connection conn, Card<? extends Duck> entity) throws SQLException {
        String sql = "INSERT INTO card_members (card_id, duck_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Duck duck : entity.getMembri()) {
                ps.setLong(1, entity.getId());
                ps.setLong(2, duck.getId());
                ps.addBatch(); // Batching pentru eficienta
            }
            ps.executeBatch();
        }
    }

    /**
     * Extrage un obiect Card (polimorfic, fara membri) din ResultSet.
     */
    private Card<? extends Duck> extractCard(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String tipCard = rs.getString("tip_card").trim();
        String numeCard = rs.getString("nume_card").trim();

        Card<? extends Duck> card;

        // Logica de Polimorfism: Instantiaza clasa concreta
        if (tipCard.equalsIgnoreCase("SwimmingCard")) {
            card = new SwimmingCard(numeCard);
        } else if (tipCard.equalsIgnoreCase("FlyingCard")) {
            card = new FlyingCard(numeCard);
        } else {
            throw new SQLException("Tip de Card necunoscut: " + tipCard);
        }
        card.setId(id);
        return card;
    }

    // --- IMPLEMENTAREA CONTRACTULUI REPOSITORY ---

    /**
     * Obtine lista de ID-uri de Duck asociate unui Card din tabela de asociere.
     * @param cardId ID-ul Cardului.
     * @return Lista de ID-uri Long ale membrilor (Duck-urilor) din Card.
     */
    @Override
    public List<Long> getMemberIdsForCard(Long cardId) {
        if (cardId == null) {
            return Collections.emptyList();
        }

        List<Long> memberIds = new ArrayList<>();
        String sql = "SELECT duck_id FROM card_members WHERE card_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, cardId);

            try (ResultSet rs = ps.executeQuery()) {
                // Nu se folosesc stream-uri, fiind o operatie JDBC imperativa
                while (rs.next()) {
                    memberIds.add(rs.getLong("duck_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare SQL la citirea membrilor Cardului " + cardId + ": " + e.getMessage());
            return Collections.emptyList();
        }
        return memberIds;
    }

    @Override
    public Optional<Card<? extends Duck>> findOne(Long id) {
        String sql = "SELECT * FROM cards WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(extractCard(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare la citirea Cardului: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Card<? extends Duck>> save(Card<? extends Duck> entity) {
        if (entity == null) throw new IllegalArgumentException("Entitatea nu poate fi nula.");
        validator.validate(entity);

        try (Connection connection = getConnection()) {
            // Nu avem implementata generarea ID-ului (presupunem ca este setat de Service sau Sequence)
            if (entity.getId() == null) {
                // ATENTIE: Aici ar trebui sa existe logica reala de getNextId(connection) din BD
                throw new RepositoryException("ID-ul Cardului nu poate fi null la salvare.");
            }

            // Incepe Tranzactia
            connection.setAutoCommit(false);

            // 1. INSERT/UPDATE Tabela CARDS (folosind ON CONFLICT DO UPDATE)
            String sqlCard = "INSERT INTO cards (id, nume_card, tip_card) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET nume_card=EXCLUDED.nume_card, tip_card=EXCLUDED.tip_card";
            try (PreparedStatement ps = connection.prepareStatement(sqlCard)) {
                ps.setLong(1, entity.getId());
                ps.setString(2, entity.getNumeCard());
                ps.setString(3, entity.getClass().getSimpleName());
                ps.executeUpdate();
            }

            // 2. Sterge vechii membri din tabela de asociere (M:N)
            deleteMembers(connection, entity.getId());

            // 3. Insereaza noii membri (daca exista)
            if (!entity.getMembri().isEmpty()) {
                insertMembers(connection, entity);
            }

            connection.commit(); // Finalizeaza tranzactia
            return Optional.empty();

        } catch (SQLException e) {
            System.err.println("Eroare SQL la salvarea Cardului. Se presupune Rollback: " + e.getMessage());
            return Optional.of(entity);
        }
    }

    @Override
    public Iterable<Card<? extends Duck>> findAll() {
        List<Card<? extends Duck>> cards = new ArrayList<>();
        String sql = "SELECT * FROM cards";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Extrage cardul de baza (fara membri)
                Card<? extends Duck> card = extractCard(rs);
                if (card != null) {
                    cards.add(card);
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare la citirea Cardurilor din baza de date: " + e.getMessage());
        }
        // NOTA: Membrii vor fi incarcati separat in Service, folosind getMemberIdsForCard.
        return cards;
    }

    @Override
    public Optional<Card<? extends Duck>> delete(Long id) {
        if (id == null) throw new IllegalArgumentException("ID-ul nu poate fi nul.");

        Optional<Card<? extends Duck>> card = findOne(id);
        if (card.isEmpty()) return Optional.empty();

        String sql = "DELETE FROM cards WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);
            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                // Stergerea din 'cards' va sterge si din 'card_members' prin ON DELETE CASCADE
                return card;
            }
        } catch (SQLException e) {
            System.err.println("Eroare la stergerea Cardului din baza de date: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public long size() {
        String sql = "SELECT COUNT(*) FROM cards";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("Eroare SQL la numararea Cardurilor: " + e.getMessage());
        }
        return 0;
    }
}