package com.ubb.repository;

import com.ubb.domain.Event;
import com.ubb.domain.Observer;
import com.ubb.domain.RaceEvent;
import com.ubb.domain.User;
import com.ubb.domain.validation.Validator;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository care gestioneaza persistenta entitatilor de tip Event in baza de date PostgreSQL.
 * Include logica de persistenta M:N pentru abonatii unui eveniment (event_subscribers).
 */
public class EventDBRepository implements EventRepositoryInterface{

    private final String url;
    private final String username;
    private final String password;
    private final Validator<Event> validator;

    /**
     * Constructor pentru initializarea conexiunii la baza de date.
     */
    public EventDBRepository(String url, String username, String password, Validator<Event> validator) {
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

    // --- METODE HELPER PENTRU ID si M:N ---

    /**
     * Interogheaza baza de date pentru a gasi urmatorul ID disponibil.
     */
    private Long getNextId(Connection connection) throws SQLException {
        String sql = "SELECT MAX(id) FROM events";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                long maxId = rs.getLong(1);
                return maxId + 1;
            }
        }
        return 1L;
    }

    /**
     * Sterge toti abonatii vechi din tabela de asociere (M:N).
     */
    private void deleteSubscribers(Connection conn, Long eventId) throws SQLException {
        String sql = "DELETE FROM event_subscribers WHERE event_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, eventId);
            ps.executeUpdate();
        }
    }

    /**
     * Insereaza noii abonati in tabela de asociere (M:N) folosind Batch.
     */
    private void insertSubscribers(Connection conn, Event entity) throws SQLException {
        String sql = "INSERT INTO event_subscribers (event_id, user_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Iteram peste abonati si adaugam in batch (folosim instance of pentru siguranta)
            for (Observer<String> observer : entity.getSubscribers()) {
                if (observer instanceof User user) {
                    ps.setLong(1, entity.getId());
                    ps.setLong(2, user.getId()); // Accesam getId() de pe User
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    /**
     * Extrage un obiect Event (polimorfic) din ResultSet.
     */
    private Event extractEvent(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String tipEvent = rs.getString("tip_event").trim();
        String name = rs.getString("nume").trim();
        String distancesStr = rs.getString("beacon_distances");
        boolean isFinished = rs.getBoolean("is_finished");

        Event event;

        // Reconstruieste lista de distante pentru RaceEvent (OPERATIE PE STREAM-URI)
        List<Double> beaconDistances = new ArrayList<>();
        if (distancesStr != null && !distancesStr.isEmpty()) {
            beaconDistances = Arrays.stream(distancesStr.split(","))
                    .map(String::trim)
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());
        }
        int numParticipants = beaconDistances.size();

        // Logica de Polimorfism: Instantiaza clasa concreta
        if (tipEvent.equalsIgnoreCase("RaceEvent")) {
            event = new RaceEvent(name, numParticipants, Collections.emptyList(), beaconDistances);
        } else {
            // Arunca eroare daca tipul nu este cunoscut
            throw new SQLException("Tip de Eveniment necunoscut: " + tipEvent);
        }

        event.setId(id);
        event.setFinished(isFinished);
        return event;
    }

    // --- IMPLEMENTAREA CONTRACTULUI REPOSITORY ---

    @Override
    public Optional<Event> findOne(Long id) {
        String sql = "SELECT * FROM events WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(extractEvent(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare la citirea Evenimentului: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Event> save(Event entity) {
        if (entity == null) throw new IllegalArgumentException("Entitatea nu poate fi nula.");
        validator.validate(entity);

        try (Connection connection = getConnection()) {

            // 1. GESTIONARE ID: Genereaza ID-ul daca este un eveniment nou
            if (entity.getId() == null) {
                entity.setId(getNextId(connection));
            }

            // Incepe tranzactia (pentru a asigura ca events si event_subscribers sunt sincronizate)
            connection.setAutoCommit(false);

            // 2. Pregatire date specifice RaceEvent (Distantele balizelor)
            String distancesStr = "";
            if (entity instanceof RaceEvent raceEvent) {
                // OPERATIE PE STREAM-URI: Convertește lista de Double in String
                distancesStr = raceEvent.getBeaconDistances().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
            }

            // 3. INSERT/UPDATE Tabela EVENTS (folosind ON CONFLICT DO UPDATE)
            String sqlEvent = "INSERT INTO events (id, nume, tip_event, beacon_distances,is_finished) VALUES (?, ?, ?, ?,?) ON CONFLICT (id) DO UPDATE SET nume=EXCLUDED.nume, tip_event=EXCLUDED.tip_event, beacon_distances=EXCLUDED.beacon_distances,is_finished=EXCLUDED.is_finished";
            try (PreparedStatement ps = connection.prepareStatement(sqlEvent)) {
                ps.setLong(1, entity.getId());
                ps.setString(2, entity.getName());
                ps.setString(3, entity.getClass().getSimpleName());
                ps.setString(4, distancesStr);
                ps.setBoolean(5, entity.isFinished());
                ps.executeUpdate();
            }

            // 4. Sterge abonatii vechi si insereaza noii abonati (Tabela M:N)
            deleteSubscribers(connection, entity.getId());
            if (!entity.getSubscribers().isEmpty()) {
                insertSubscribers(connection, entity);
            }

            connection.commit();
            return Optional.empty();

        } catch (SQLException e) {
            System.err.println("Eroare SQL la salvarea Evenimentului. Se face Rollback: " + e.getMessage());
            return Optional.of(entity);
        }
    }

    @Override
    public Optional<Event> delete(Long id) {
        Optional<Event> event = findOne(id);
        if (event.isEmpty()) return Optional.empty();

        // Tabela event_subscribers se sterge automat prin ON DELETE CASCADE in baza de date
        String sql = "DELETE FROM events WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);
            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                return event;
            }
        } catch (SQLException e) {
            System.err.println("Eroare la stergerea Evenimentului: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Iterable<Event> findAll() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                events.add(extractEvent(rs));
            }
        } catch (SQLException e) {
            System.err.println("Eroare la citirea Evenimentelor: " + e.getMessage());
        }
        return events;
    }

    @Override
    public long size() {
        String sql = "SELECT COUNT(*) FROM events";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("Eroare la numarare: " + e.getMessage());
        }
        return 0;
    }

    // --- METODE SPECIFICE EVENT REPOSITORY INTERFACE ---

    /**
     * Returneaza ID-urile abonatilor pentru un eveniment specificat din tabela M:N.
     * @param eventId ID-ul evenimentului.
     * @return Lista de ID-uri Long ale abonatilor.
     */
    @Override
    public List<Long> getSubscriberIdsForEvent(Long eventId) {
        if (eventId == null) {
            return Collections.emptyList();
        }

        List<Long> subscriberIds = new ArrayList<>();
        // Interogarea selecteaza user_id din tabela de asociere
        String sql = "SELECT user_id FROM event_subscribers WHERE event_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Adauga ID-ul de User gasit
                    subscriberIds.add(rs.getLong("user_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare SQL la citirea abonatilor Evenimentului " + eventId + ": " + e.getMessage());
            return Collections.emptyList();
        }

        return subscriberIds;
    }


    @Override
    public List<Event> findFinishedEventsByUserId(Long userId) {
        List<Event> events = new ArrayList<>();
        // SQL care verifica evenimentele terminate la care user-ul este abonat
        String sql = "SELECT e.* FROM events e " +
                "JOIN event_subscribers es ON e.id = es.event_id " +
                "WHERE es.user_id = ? AND e.is_finished = true";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    events.add(extractEvent(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare la filtrarea offline: " + e.getMessage());
        }
        return events;
    }
}