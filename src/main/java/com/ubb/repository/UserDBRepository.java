package com.ubb.repository;

import com.ubb.domain.*;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.exceptions.RepositoryException;
import com.ubb.utils.paging.Page;
import com.ubb.utils.paging.Pageable;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repository care gestioneaza persistenta entitatilor de tip User in baza de date PostgreSQL.
 * Implementeaza UserRepositoryInterface.
 */
public class UserDBRepository implements UserRepositoryInterface {

    private final String url;
    private final String username;
    private final String password;
    private final Validator<User> validator;

    /**
     * Constructor pentru initializarea conexiunii la baza de date.
     */
    public UserDBRepository(String url, String username, String password, Validator<User> validator) {
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


    // --- METODE HELPER DE MAPARE SI LOGICA INTERNA ---

    /**
     * Interogheaza baza de date pentru a gasi urmatorul ID disponibil (MAX(id) + 1).
     */
    private Long getNextId(Connection connection) throws SQLException {
        // Aici nu se folosesc stream-uri, fiind o operatie pe baza de date.
        String sql = "SELECT MAX(id) FROM users";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                long maxId = rs.getLong(1);
                return maxId + 1;
            }
        }
        return 1L; // Fallback in cazul in care tabela este goala
    }

    /**
     * Extrage un obiect User (Persoana, SwimmingDuck, etc.) din ResultSet.
     */
    private User extractUser(ResultSet resultSet) throws SQLException {
        Long id = resultSet.getLong("id");
        String tipUser = resultSet.getString("tip_user").trim();
        String userN = resultSet.getString("username").trim();
        String email = resultSet.getString("email").trim();
        String passwordHash = resultSet.getString("password").trim();

        User user = null;

        // Deserializarea obiectului bazata pe tip_user (polimorfism)
        if (tipUser.equalsIgnoreCase("Persoana")) {
            user = new Persoana(
                    userN, email, passwordHash,
                    resultSet.getString("nume").trim(),
                    resultSet.getString("prenume").trim(),
                    resultSet.getDate("data_nasterii").toLocalDate(),
                    resultSet.getString("ocupatie").trim(),
                    resultSet.getInt("nivel_empatie")
            );
        } else if (tipUser.contains("Duck")) { // Folosim contains pentru a prinde toate subtipurile de rata
            double viteza = resultSet.getDouble("viteza");
            double rezistenta = resultSet.getDouble("rezistenta");

            if (tipUser.equalsIgnoreCase("SwimmingDuck")) {
                user = new SwimmingDuck(userN, email, passwordHash, viteza, rezistenta);
            } else if (tipUser.equalsIgnoreCase("FlyingDuck")) {
                user = new FlyingDuck(userN, email, passwordHash, viteza, rezistenta);
            }
            else if (tipUser.equalsIgnoreCase("FlyingAndSwimmingDuck")) {
                user = new FlyingAndSwimmingDuck(userN, email, passwordHash, viteza, rezistenta);
            }
        }

        if (user != null) {
            user.setId(id);
            user.setImagePath(resultSet.getString("image_path"));
        }
        return user;
    }

    /**
     * Seteaza parametrii comuni si specifici pentru interogarile INSERT/UPDATE (JDBC).
     * @param ps PreparedStatement-ul.
     * @param entity Entitatea User.
     * @param startIdx Indexul de start pentru parametri.
     * @return Urmatorul index disponibil in PreparedStatement.
     */
    private int setAllUserParams(PreparedStatement ps, User entity, int startIdx) throws SQLException {
        ps.setString(startIdx++, entity.getUsername());
        ps.setString(startIdx++, entity.getEmail());
        ps.setString(startIdx++, entity.getPasswordHash());
        ps.setString(startIdx++, entity.getClass().getSimpleName()); // Tipul concret al clasei

        // Parametrii Persoana / Duck
        if (entity instanceof Persoana p) {
            ps.setString(startIdx++, p.getNume());
            ps.setString(startIdx++, p.getPrenume());
            ps.setDate(startIdx++, Date.valueOf(p.getDataNasterii()));
            ps.setString(startIdx++, p.getOcupatie());
            ps.setInt(startIdx++, p.getNivelEmpatie());
            ps.setObject(startIdx++, null, Types.DOUBLE); // Viteza (NULL)
            ps.setObject(startIdx++, null, Types.DOUBLE); // Rezistenta (NULL)
        } else if (entity instanceof Duck d) {
            // Campuri Persoana (NULL)
            ps.setObject(startIdx++, null);
            ps.setObject(startIdx++, null);
            ps.setObject(startIdx++, null);
            ps.setObject(startIdx++, null);
            ps.setObject(startIdx++, null);
            // Campuri Duck
            ps.setDouble(startIdx++, d.getViteza());
            ps.setDouble(startIdx++, d.getRezistenta());
        }
        ps.setString(startIdx++, entity.getImagePath());
        return startIdx;
    }

    /**
     * Metoda privata de UPDATE (inlocuieste save daca entitatea exista).
     */
    private Optional<User> update(User entity) {
        String sql = "UPDATE users SET username=?, email=?, password=?, tip_user=?, nume=?, prenume=?, data_nasterii=?, ocupatie=?, nivel_empatie=?, viteza=?, rezistenta=?, image_path=? WHERE id=?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            int nextIdx = setAllUserParams(ps, entity, 1);
            ps.setLong(nextIdx, entity.getId()); // Seteaza ID-ul pentru clauza WHERE

            ps.executeUpdate();

            return Optional.empty(); // Update reusit

        } catch (SQLException e) {
            System.err.println("Eroare SQL la actualizarea (UPDATE) utilizatorului: " + e.getMessage());
            return Optional.of(entity); // Returneaza entitatea care nu a putut fi actualizata
        }
    }

    // --- METODE REPOSITORY (IMPLEMENTAREA CONTRACTULUI) ---

    @Override
    public Optional<User> findOne(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(extractUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare la citirea din baza de date: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Iterable<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User user = extractUser(rs);
                if (user != null) {
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare la citirea din baza de date: " + e.getMessage());
        }
        return users;
    }

    @Override
    public Optional<User> save(User entity) {
        if (entity == null) throw new IllegalArgumentException("Entitatea nu poate fi nula.");

        validator.validate(entity);

        try (Connection connection = getConnection()) {
            // 1. Genereaza ID-ul daca este un utilizator nou (id == null)
            if (entity.getId() == null) {
                entity.setId(getNextId(connection));
            }

            // 2. Verifica daca utilizatorul exista deja
            if (findOne(entity.getId()).isPresent()) {
                // Daca exista, facem UPDATE si returnam rezultatul
                return update(entity);
            }

            // 3. Executa INSERT
            String sql = "INSERT INTO users (id, username, email, password, tip_user, nume, prenume, data_nasterii, ocupatie, nivel_empatie, viteza, rezistenta,image_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, entity.getId());
                setAllUserParams(ps, entity, 2); // Seteaza restul parametrilor incepand cu index 2
                ps.executeUpdate();
                return Optional.empty(); // Salvare (INSERT) reusita
            }

        } catch (SQLException e) {
            System.err.println("Eroare SQL la salvarea (INSERT) utilizatorului: " + e.getMessage());
        }
        return Optional.of(entity); // Returneaza entitatea daca a aparut o eroare
    }

    @Override
    public Optional<User> delete(Long id) {
        if (id == null) throw new IllegalArgumentException("ID-ul nu poate fi nul.");

        Optional<User> user = findOne(id);
        if (user.isEmpty()) return Optional.empty();

        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);
            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Eroare la stergerea din baza de date: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public long size() {
        String sql = "SELECT COUNT(*) FROM users";
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

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username-ul nu poate fi null.");
        }

        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(extractUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare SQL la cautarea dupa username: " + e.getMessage());
        }
        return Optional.empty();
    }

    // --- METODE SPECIFICE USER REPOSITORY INTERFACE ---

    @Override
    public List<Duck> findDucksByType(String userType) {
        List<Duck> filteredDucks = new ArrayList<>();
        // Asiguram ca tipul este corect (ex: 'SwimmingDuck', nu 'SwimmingDucks')
        String dbType = userType.replace("s", "");

        String sql = "SELECT * FROM users WHERE tip_user = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, dbType);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = extractUser(rs);
                    // Casting-ul este sigur daca extractUser a mers bine
                    if (user instanceof Duck duck) {
                        filteredDucks.add(duck);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare SQL la filtrare dupa tip: " + e.getMessage());
        }
        return filteredDucks;
    }

    @Override
    public List<Duck> findAllDucks() {
        List<Duck> allDucks = new ArrayList<>();
        // Interogare optimizata: Selecteaza toti utilizatorii unde tip_user contine 'Duck'
        String sql = "SELECT * FROM users WHERE tip_user LIKE '%Duck%'";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                User user = extractUser(rs);
                if (user instanceof Duck duck) {
                    allDucks.add(duck);
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare SQL la citirea tuturor ratelor: " + e.getMessage());
        }
        return allDucks;
    }

    // --- METODE PAGING REPOSITORY ---

    @Override
    public long count(){
        return size(); // count() este echivalent cu size() in aceasta implementare
    }

    /**
     * Metoda privata utilitara pentru a mapa tipul de filtru din Service la valoarea din baza de date.
     */
    private String mapFilterTypeToDbType(String filterType) {
        if (filterType == null || "Toate".equalsIgnoreCase(filterType)) return null;
        return switch (filterType) {
            case "Persoane" -> "Persoana";
            case "Ducks" -> "DuckGroup"; // Indicator pentru LIKE
            case "SwimmingDucks" -> "SwimmingDuck";
            case "FlyingDucks" -> "FlyingDuck";
            case "FlyingAndSwimmingDucks" -> "FlyingAndSwimmingDuck";
            default -> filterType;
        };
    }

    @Override
    public List<User> findAll(Pageable pageable){
        return findAllPaginated(pageable, null); // Apel catre metoda interna fara filtrare
    }


    /**
     * Extrage o pagina de utilizatori, filtrata eficient prin SQL (WHERE, LIMIT, OFFSET).
     */
    @Override
    public Page<User> findUsersPaginatedAndFiltered(Pageable pageable, String filterType) {
        String dbTypeIndicator = mapFilterTypeToDbType(filterType);

        // 1. Identificăm tipul de filtrare pentru a construi SQL-ul corect
        boolean isGroupDuck = "DuckGroup".equals(dbTypeIndicator);
        boolean isSpecificType = dbTypeIndicator != null && !isGroupDuck;

        long totalElements = 0;
        String countSql = "SELECT COUNT(*) FROM users";

        if (isGroupDuck) {
            countSql += " WHERE tip_user LIKE '%Duck%'";
        } else if (isSpecificType) {
            countSql += " WHERE tip_user = ?";
        }

        try (Connection connection = getConnection();
             PreparedStatement psCount = connection.prepareStatement(countSql)) {

            // Setează parametrul DOAR dacă filtrarea este specifică (ex: Persoana, SwimmingDuck)
            if (isSpecificType) {
                psCount.setString(1, dbTypeIndicator);
            }

            try (ResultSet rsCount = psCount.executeQuery()) {
                if (rsCount.next()) {
                    totalElements = rsCount.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare SQL la COUNT: " + e.getMessage());
            return new Page<>(Collections.emptyList(), pageable.getPageNumber(), 0, 0);
        }

        // 2. Calculează metadatele paginii
        int pageSize = pageable.getPageSize();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int currentPageNumber = pageable.getPageNumber();

        // Validare: dacă pagina solicitată este invalidă sau nu există date
        if (totalElements == 0 || currentPageNumber > totalPages || currentPageNumber < 1) {
            return new Page<>(Collections.emptyList(), currentPageNumber, totalPages, totalElements);
        }

        // 3. Interogarea de conținut folosind metoda helper
        List<User> users = findAllPaginated(pageable, dbTypeIndicator);

        return new Page<>(users, currentPageNumber, totalPages, totalElements);
    }

    /**
     * Extrage lista de utilizatori pentru pagina curentă cu suport pentru filtrare polimorfică.
     */
    private List<User> findAllPaginated(Pageable pageable, String dbTypeIndicator) {
        List<User> users = new ArrayList<>();

        // UI pornește de la 1, SQL OFFSET pornește de la 0
        int offset = (pageable.getPageNumber() - 1) * pageable.getPageSize();

        boolean isGroupDuck = "DuckGroup".equals(dbTypeIndicator);
        boolean isSpecificType = dbTypeIndicator != null && !isGroupDuck;

        StringBuilder sql = new StringBuilder("SELECT * FROM users");

        if (isGroupDuck) {
            sql.append(" WHERE tip_user LIKE '%Duck%'");
        } else if (isSpecificType) {
            sql.append(" WHERE tip_user = ?");
        }

        sql.append(" ORDER BY id LIMIT ? OFFSET ?");

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {

            int paramIdx = 1;

            // Adăugăm parametrul de tip doar dacă nu este filtrare pe grup (LIKE deja inclus în SQL)
            if (isSpecificType) {
                ps.setString(paramIdx++, dbTypeIndicator);
            }

            // Parametrii pentru LIMIT și OFFSET
            ps.setInt(paramIdx++, pageable.getPageSize());
            ps.setInt(paramIdx, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = extractUser(rs);
                    if (user != null) {
                        users.add(user);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Eroare SQL la findAllPaginated: " + e.getMessage());
        }
        return users;
    }


    public void updateProfileImage(Long userId, String newImagePath) {
        // Interogarea vizează DOAR coloana image_path
        String sql = "UPDATE users SET image_path = ? WHERE id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, newImagePath);
            ps.setLong(2, userId);

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Eroare SQL la actualizarea pozei: " + e.getMessage());
        }
    }
}