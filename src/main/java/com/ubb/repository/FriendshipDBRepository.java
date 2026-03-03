package com.ubb.repository;

import com.ubb.domain.Friendship;
import com.ubb.domain.Tuple;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.exceptions.RepositoryException;
import com.ubb.utils.dto.FriendshipDTO;
import com.ubb.utils.paging.Page;
import com.ubb.utils.paging.Pageable;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository care gestioneaza persistenta entitatilor de tip Friendship in baza de date PostgreSQL.
 * ID-ul compus (Tuple<Long, Long>) este stocat ca doua coloane (id1, id2).
 */
public class FriendshipDBRepository implements FriendshipRepositoryInterface {

    private final String url;
    private final String username;
    private final String password;
    private final Validator<Friendship> validator;

    /**
     * Constructor pentru initializarea conexiunii la baza de date.
     */
    public FriendshipDBRepository(String url, String username, String password, Validator<Friendship> validator) {
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

    // --- METODE HELPER DE MAPARE ---

    /**
     * Extrage un obiect Friendship din ResultSet.
     */
    private Friendship extractFriendship(ResultSet rs) throws SQLException {
        Long id1 = rs.getLong("id1");
        Long id2 = rs.getLong("id2");

        // Extrage Timestamp-ul din SQL
        Timestamp timestamp = rs.getTimestamp("friends_from");
        LocalDateTime friendsFrom = (timestamp != null) ? timestamp.toLocalDateTime() : null;

        // Constructorul Friendship se asigura ca ID-ul compus (Tuple) este standardizat (ID mic, ID mare).
        return new Friendship(id1, id2, friendsFrom);
    }

    // --- IMPLEMENTAREA CONTRACTULUI REPOSITORY ---

    @Override
    public Optional<Friendship> findOne(Tuple<Long, Long> id) {
        if (id == null) throw new IllegalArgumentException("ID-ul nu poate fi nul.");

        // ID-ul compus din Repo este mereu standardizat (id mic, id mare).
        // Cautam perechea exacta in baza de date.
        String sql = "SELECT * FROM friendships WHERE id1 = ? AND id2 = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id.getFirst());
            ps.setLong(2, id.getSecond());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(extractFriendship(rs));
            }
        } catch (SQLException e) {
            System.err.println("Eroare la citirea din baza de date (Friendship): " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Friendship> save(Friendship entity) {
        if (entity == null) throw new IllegalArgumentException("Entitatea nu poate fi nula.");

        validator.validate(entity);

        // Verificam daca prietenia exista deja (Friendship nu ar trebui sa aiba update)
        if (findOne(entity.getId()).isPresent()) {
            // Returneaza entitatea existenta daca prietenia nu poate fi salvata (ID duplicat)
            return Optional.of(entity);
        }

        // Interogare INSERT (foloseste ID-urile standardizate din entitate)
        String sql = "INSERT INTO friendships (id1, id2, friends_from) VALUES (?, ?, ?)";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            // Folosim getIdUser1/2 care asigura ordinea standardizata (ID mic, ID mare)
            ps.setLong(1, entity.getIdUser1());
            ps.setLong(2, entity.getIdUser2());

            // Seteaza TIMESTAMP
            ps.setTimestamp(3, Timestamp.valueOf(entity.getFriendsFrom()));

            ps.executeUpdate();

            return Optional.empty(); // Salvare (INSERT) reusita

        } catch (SQLException e) {
            System.err.println("Eroare SQL la salvarea (INSERT) prieteniei: " + e.getMessage());
        }
        return Optional.of(entity); // Returneaza entitatea daca a aparut o eroare SQL
    }

    @Override
    public Optional<Friendship> delete(Tuple<Long, Long> id) {
        if (id == null) throw new IllegalArgumentException("ID-ul nu poate fi nul.");

        Optional<Friendship> friendship = findOne(id);
        if (friendship.isEmpty()) return Optional.empty();

        String sql = "DELETE FROM friendships WHERE id1 = ? AND id2 = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            // Folosim ID-urile standardizate pentru stergere
            ps.setLong(1, id.getFirst());
            ps.setLong(2, id.getSecond());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                return friendship; // Returneaza entitatea stearsa
            }
        } catch (SQLException e) {
            System.err.println("Eroare la stergerea din baza de date: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Iterable<Friendship> findAll() {
        List<Friendship> friendships = new ArrayList<>();
        String sql = "SELECT * FROM friendships";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                friendships.add(extractFriendship(rs));
            }
        } catch (SQLException e) {
            System.err.println("Eroare la citirea din baza de date: " + e.getMessage());
        }
        return friendships;
    }

    @Override
    public long size() {
        String sql = "SELECT COUNT(*) FROM friendships";
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

    // --- METODE PAGING REPOSITORY ---

    @Override
    public long count() {
        return size(); // count() este echivalent cu size() in aceasta implementare
    }

    @Override
    public List<Friendship> findAll(Pageable pageable) {
        List<Friendship> friendships = new ArrayList<>();

        int offset = (pageable.getPageNumber() - 1) * pageable.getPageSize();

        // Prieteniile nu au o ordine naturala, dar folosim ORDER BY friends_from DESC
        // pentru a asigura o ordonare consistenta pe pagini.
        String sql = "SELECT * FROM friendships ORDER BY friends_from DESC LIMIT ? OFFSET ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, pageable.getPageSize());
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    friendships.add(extractFriendship(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare SQL la paginarea prieteniilor: " + e.getMessage());
        }
        return friendships;
    }

    @Override
    public List<Long> findFriendsOf(Long userId) {
        List<Long> friendIds = new ArrayList<>();

        // SQL: Cauta userId in id1 SAU id2
        String sql = "SELECT id1, id2 FROM friendships WHERE id1 = ? OR id2 = ?";

        try (Connection connection = getConnection(); // presupunem ca getConnection() este disponibil
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long user1Id = rs.getLong("id1");
                    Long user2Id = rs.getLong("id2");

                    // Identificam ID-ul partenerului (cel care nu este userId)
                    Long friendId = user1Id.equals(userId) ? user2Id : user1Id;

                    friendIds.add(friendId);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Eroare SQL la extragerea ID-urilor prietenilor: " + e.getMessage(), e);
        }
        return friendIds;
    }

    /**
     * Extrage toate obiectele Friendship pentru un anumit utilizator.
     */
    @Override
    public List<Friendship> findFriendshipsOf(Long userId) {
        List<Friendship> friendships = new ArrayList<>();
        String sql = "SELECT id1, id2, friends_from FROM friendships WHERE id1 = ? OR id2 = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long user1Id = rs.getLong("id1");
                    Long user2Id = rs.getLong("id2");
                    LocalDateTime date = rs.getTimestamp("friends_from").toLocalDateTime();

                    // Reconstituirea obiectului Friendship
                    Friendship friendship = new Friendship(user1Id, user2Id);
                    friendship.setFriendsFrom(date);
                    friendships.add(friendship);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Eroare SQL la extragerea obiectelor Friendship: " + e.getMessage(), e);
        }
        return friendships;
    }

    @Override
    public Page<FriendshipDTO> findAllPaginatedForUser(Pageable pageable, Long userId) {
        List<FriendshipDTO> friendships = new ArrayList<>();
        int pageSize = pageable.getPageSize();
        int offset = (pageable.getPageNumber() - 1) * pageSize;

        // SQL care aduce ID-urile și Username-urile ambelor părți
        String dataSql = "SELECT f.id1, u1.username AS name1, f.id2, u2.username AS name2, f.friends_from " +
                "FROM friendships f " +
                "JOIN users u1 ON f.id1 = u1.id " +
                "JOIN users u2 ON f.id2 = u2.id " +
                "WHERE f.id1 = ? OR f.id2 = ? " +
                "LIMIT ? OFFSET ?";

        String countSql = "SELECT COUNT(*) FROM friendships WHERE id1 = ? OR id2 = ?";
        long totalElements = 0;

        try (Connection connection = getConnection()) {
            // 1. Numărare
            try (PreparedStatement psCount = connection.prepareStatement(countSql)) {
                psCount.setLong(1, userId);
                psCount.setLong(2, userId);
                ResultSet rs = psCount.executeQuery();
                if (rs.next()) totalElements = rs.getLong(1);
            }

            // 2. Extracție date cu JOIN
            try (PreparedStatement psData = connection.prepareStatement(dataSql)) {
                psData.setLong(1, userId);
                psData.setLong(2, userId);
                psData.setInt(3, pageSize);
                psData.setInt(4, offset);

                ResultSet rs = psData.executeQuery();
                while (rs.next()) {
                    // Folosim constructorul tău specific
                    friendships.add(new FriendshipDTO(
                            rs.getLong("id1"),
                            rs.getString("name1"),
                            rs.getLong("id2"),
                            rs.getString("name2"),
                            rs.getTimestamp("friends_from").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        return new Page<>(friendships, pageable.getPageNumber(), totalPages, totalElements);
    }
}