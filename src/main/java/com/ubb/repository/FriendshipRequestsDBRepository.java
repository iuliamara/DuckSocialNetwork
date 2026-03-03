package com.ubb.repository;

import com.ubb.domain.FriendshipRequest;
import com.ubb.domain.Message;
import com.ubb.domain.RequestStatus;
import com.ubb.domain.User;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.exceptions.RepositoryException;
import com.ubb.service.exceptions.EntityNotFoundException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FriendshipRequestsDBRepository implements FriendshipRequestRepositoryInterface {
    private final String url;
    private final String username;
    private final String password;
    private final Validator<FriendshipRequest> validator;

    private final UserRepositoryInterface userRepository;

    public FriendshipRequestsDBRepository(String url, String username, String password, Validator<FriendshipRequest> validator, UserRepositoryInterface userRepository) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.validator = validator;
        this.userRepository = userRepository;
    }

    /**
     * Obtine o noua conexiune la baza de date.
     */
    private Connection getConnection() throws SQLException{
        return DriverManager.getConnection(url,username,password);
    }

    /**
     * Extrage entitatea FriendshipRequest dintr-un ResultSet si hidrateaza obiectele User.
     */
    private FriendshipRequest extractEntity(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long from_user_id = rs.getLong("from_user_id");
        Long to_user_id = rs.getLong("to_user_id");
        String statusStr = rs.getString("status");
        Timestamp date_sent_TS=rs.getTimestamp("date_sent");
        boolean isViewed = rs.getBoolean("is_viewed_by_recipient");
        LocalDateTime dateSent=date_sent_TS.toLocalDateTime();

        User fromUser = userRepository.findOne(from_user_id)
                .orElseThrow(() -> new EntityNotFoundException("User-ul expeditor " + from_user_id + " nu a fost gasit la extragerea cererii."));
        User toUser=userRepository.findOne(to_user_id)
                .orElseThrow(()->new EntityNotFoundException("User-ul destinatar " + to_user_id + " nu a fost gasit la extragerea cererii."));
        RequestStatus status = RequestStatus.valueOf(statusStr);

        //Reconstructia entitatii
        return new FriendshipRequest(id,fromUser,toUser,status,dateSent,isViewed);
    }

    // --- CRUD DE BAZA ---
    public Optional<FriendshipRequest> findOne(Long id) throws EntityNotFoundException{
        String query = "SELECT * FROM FRIENDSHIPREQUESTS WHERE ID = ?";
        try(Connection connection= getConnection();
            PreparedStatement ps = connection.prepareStatement(query);){
            ps.setLong(1,id);
            try(ResultSet rs=ps.executeQuery()){
                if(rs.next()) return Optional.of(extractEntity(rs));
            }
        } catch (SQLException e){
            throw new RepositoryException("Eroare la citirea din BD" + e.getMessage());
        }
        return Optional.empty();
    }

    public List<FriendshipRequest> findAll() {
        List<FriendshipRequest> requests= new ArrayList<>();
        String query = "SELECT * FROM FRIENDSHIPREQUESTS";
        try(Connection connection=getConnection();){
            PreparedStatement ps=connection.prepareStatement(query);
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()){
                    FriendshipRequest request=extractEntity(rs);
                    requests.add(request);
                }
            }
        } catch(SQLException e){
            throw new RepositoryException("Eroare la citirea din BD " + e.getMessage());
        }
        return requests;
    }

    public Optional<FriendshipRequest> save(FriendshipRequest entity) {
        if (entity == null) throw new IllegalArgumentException("Entitatea nu poate fi nula");
        validator.validate(entity);

        if (entity.getId() == null) {
            // NOU: Logica pentru cerere noua (INSERT)
            String query = "INSERT INTO FRIENDSHIPREQUESTS(from_user_id, to_user_id, status, date_sent, is_viewed_by_recipient) VALUES (?,?,?,?,?)";

            try (Connection connection = getConnection();
                 // Adaugam RETURN_GENERATED_KEYS DACA nu este deja in getConnection()
                 PreparedStatement ps = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);) {

                ps.setLong(1, entity.getFromUser().getId());
                ps.setLong(2, entity.getToUser().getId());
                ps.setString(3, entity.getStatus().toString());
                ps.setTimestamp(4, Timestamp.valueOf(entity.getDateSent()));
                ps.setBoolean(5, entity.isViewedByRecipient()); // Adaugat is_viewed_by_recipient

                int affectedRows = ps.executeUpdate();

                if (affectedRows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            entity.setId(rs.getLong(1));
                            return Optional.empty(); // <-- SUCCES: Salvare + ID
                        }
                    }
                }

                // Daca affectedRows > 0 dar nu s-a putut obtine ID-ul, consideram un esec partial/neasteptat
                // Si returnam Optional.of(entity) pentru a declansa eroarea in Service.
                // DAR: Deoarece vedem ca functioneaza, nu vrem sa ajungem aici.
                // Schimbam logica la final:

                return Optional.of(entity); // Returneaza entitatea doar in caz de esec (de ex., affectedRows=0 sau rs.next()=false)

            } catch (SQLException e) {
                // Exceptiile SQL (de ex., Unique Constraint Violation) sunt tratate aici
                throw new RepositoryException("Eroare SQL la salvarea in BD: " + e.getMessage(), e);
            }
        } else {
            // UPDATE: Actualizare status (logica pare corecta)
            String sql = "UPDATE friendshiprequests SET status = ?, date_sent = ?, is_viewed_by_recipient = ? WHERE id = ?";
            try (Connection connection = getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, entity.getStatus().toString());
                ps.setTimestamp(2, Timestamp.valueOf(entity.getDateSent()));
                ps.setBoolean(3, entity.isViewedByRecipient());
                ps.setLong(4, entity.getId());

                int affectedRows = ps.executeUpdate();
                return affectedRows == 0 ? Optional.of(entity) : Optional.empty();

            } catch (SQLException e) {
                throw new RepositoryException("Eroare SQL la actualizarea cererii: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public Optional<FriendshipRequest> delete(Long id) {
        Optional<FriendshipRequest> requestOpt = findOne(id);
        if (requestOpt.isEmpty()) {
            return Optional.empty();
        }

        String sql = "DELETE FROM friendshiprequests WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();
            return requestOpt;

        } catch (SQLException e) {
            throw new RepositoryException("Eroare la stergerea cererii cu ID " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public long size(){
        return 0;
    }

    // --- METODE SPECIFICE ---
    public List<FriendshipRequest> findRequestsByRecipientAndStatuses(Long toUserId, List<RequestStatus> statuses) {
        List<FriendshipRequest> requests= new ArrayList<>();

        //Construim stringul de statusuri pt interogarea IN (?,?..)
        String statusPlaceholders=statuses.stream()
                .map(s->"?")
                .collect(Collectors.joining(","));
        String query=String.format("SELECT * FROM FRIENDSHIPREQUESTS WHERE to_user_id=? AND status IN (%s)",statusPlaceholders);
        try(Connection connection=getConnection();
        PreparedStatement ps = connection.prepareStatement(query)){
            ps.setLong(1,toUserId);
            for(int i=0;i<statuses.size();i++){
                ps.setString(i+2,statuses.get(i).toString());
            }
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()){
                    requests.add(extractEntity(rs));
                }
            }
        }catch(SQLException e){
            throw new RepositoryException("Eroare la citirea din BD" + e.getMessage());
        }
        return requests;
    }

    @Override
    public Optional<FriendshipRequest> findRequestBySenderAndRecipient(Long fromUserId, Long toUserId) {
        String sql = "SELECT * FROM friendshiprequests WHERE from_user_id = ? AND to_user_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, fromUserId);
            ps.setLong(2, toUserId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(extractEntity(rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Eroare la cautarea cererii (sender/recipient): " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<FriendshipRequest> findRequestsBySender(Long senderId, RequestStatus status) {
        List<FriendshipRequest> requests = new ArrayList<>();

        // Baza interogarii
        String sql = "SELECT * FROM friendshiprequests WHERE from_user_id = ?";

        // Adaugam conditia pentru status, daca este specificat
        if (status != null) {
            sql += " AND status = ?";
        }

        // Adaugam o ordine logica (ex: cele mai noi primele)
        sql += " ORDER BY date_sent DESC";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, senderId);

            // Setam al doilea parametru (status), daca exista
            int paramIndex = 2;
            if (status != null) {
                ps.setString(paramIndex, status.toString());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Presupunem ca aveti o metoda de a converti ResultSet-ul in obiect
                    FriendshipRequest request = extractEntity(rs);
                    requests.add(request);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Eroare SQL la extragerea cererilor trimise: " + e.getMessage(), e);
        }
        return requests;
    }

    @Override
    public List<FriendshipRequest> findRequestsByReceiver(Long senderId, RequestStatus status) {
        List<FriendshipRequest> requests = new ArrayList<>();

        // Baza interogarii
        String sql = "SELECT * FROM friendshiprequests WHERE to_user_id = ?";

        // Adaugam conditia pentru status, daca este specificat
        if (status != null) {
            sql += " AND status = ?";
        }

        // Adaugam o ordine logica (ex: cele mai noi primele)
        sql += " ORDER BY date_sent DESC";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, senderId);

            // Setam al doilea parametru (status), daca exista
            int paramIndex = 2;
            if (status != null) {
                ps.setString(paramIndex, status.toString());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Presupunem ca aveti o metoda de a converti ResultSet-ul in obiect
                    FriendshipRequest request = extractEntity(rs);
                    requests.add(request);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("Eroare SQL la extragerea cererilor trimise: " + e.getMessage(), e);
        }
        return requests;
    }

    public int getUnreadNotificationsCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE to_user_id = ? AND is_read = false";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
