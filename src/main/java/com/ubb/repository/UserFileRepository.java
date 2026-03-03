package com.ubb.repository;

import com.ubb.domain.*;
import com.ubb.domain.exceptions.ValidationException;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.exceptions.RepositoryException;
import com.ubb.utils.paging.Page;
import com.ubb.utils.paging.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository specific pentru entitatile de tip User, care persista datele in fisier.
 * Implementeaza Template Method Pattern pentru I/O si foloseste Stream-uri pentru logica de filtrare.
 */
public class UserFileRepository extends AbstractFileRepository<Long, User> implements UserRepositoryInterface {

    // Separatorul folosit in fisier (CSV-style)
    private static final String SEPARATOR = ";";

    /**
     * Constructor.
     * @param fileName Numele fisierului de persistenta.
     * @param validator Validatorul pentru entitati de tip User.
     */
    public UserFileRepository(String fileName, Validator<User> validator) {
        super(fileName, validator);
    }


    // --- Specificatii: Hook-uri si Metode Auxiliare (cu Stream-uri) ---

    /**
     * Hook method: Metoda apelata inainte de salvare, folosita aici pentru a genera ID-ul.
     * @param entity Entitatea care urmeaza sa fie salvata.
     * @return Entitatea cu ID-ul setat (daca era null).
     */
    @Override
    protected User preSaveHook(User entity) {
        // Verificam daca ID-ul trebuie generat
        if (entity.getId() == null) {
            entity.setId(getNextId());
        }
        return entity;
    }

    /**
     * Metoda privata care gaseste urmatorul ID liber folosind operatii pe stream-uri.
     * @return Urmatorul ID Long disponibil.
     */
    private Long getNextId() {
        if (entities.isEmpty()) {
            return 1L;
        }

        // OPERATII PE STREAM-URI: Gasirea ID-ului maxim (inlocuieste un for)
        return entities.keySet().stream()
                .filter(Objects::nonNull) // Filtrare pentru siguranta
                .mapToLong(id -> id)
                .max()
                .orElse(0L) + 1;
    }

    // --- Specificatii: Template Methods ---

    /**
     * Implementarea Template Method: Extrage o entitate User (Persoana sau Duck) dintr-un rand de text.
     */
    @Override
    protected User extractEntity(String line) {
        try {
            String[] parts = line.split(SEPARATOR);
            if (parts.length < 5) {
                throw new RepositoryException("Linie incompleta sau format invalid pentru User.");
            }

            Long id = Long.parseLong(parts[0].trim());
            String tipUser = parts[1].trim();
            User user;

            // Logica de Deserializare pentru Persoana
            if (tipUser.equalsIgnoreCase("Persoana")) {
                if (parts.length < 10) throw new RepositoryException("Linie incompleta pentru Persoana.");

                user = new Persoana(
                        parts[2].trim(), // Username
                        parts[3].trim(), // Email
                        parts[4].trim(), // Password
                        parts[5].trim(), // Nume
                        parts[6].trim(), // Prenume
                        LocalDate.parse(parts[7].trim()), // DataNasterii
                        parts[8].trim(), // Ocupatie
                        Integer.parseInt(parts[9].trim()) // NivelEmpatie
                );
            }
            // Logica de Deserializare pentru Duck
            else if (tipUser.equalsIgnoreCase("Duck")) {
                if (parts.length < 8) throw new RepositoryException("Linie incompleta pentru Duck.");

                String typeDuck = parts[5].trim();
                double viteza = Double.parseDouble(parts[6].trim());
                double rezistenta = Double.parseDouble(parts[7].trim());

                if (typeDuck.equalsIgnoreCase("Swimming")) {
                    user = new SwimmingDuck(parts[2].trim(), parts[3].trim(), parts[4].trim(), viteza, rezistenta);
                } else if (typeDuck.equalsIgnoreCase("Flying")) {
                    user = new FlyingDuck(parts[2].trim(), parts[3].trim(), parts[4].trim(), viteza, rezistenta);
                }
                else if(typeDuck.equalsIgnoreCase("FlyingAndSwimming")) {
                    user = new FlyingAndSwimmingDuck(parts[2].trim(), parts[3].trim(), parts[4].trim(), viteza, rezistenta);
                }
                else {
                    throw new RepositoryException("Tip de rata necunoscut: " + typeDuck);
                }
            } else {
                throw new RepositoryException("Tip de utilizator necunoscut: " + tipUser);
            }

            user.setId(id);
            return user;

        } catch (ValidationException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new RepositoryException("Eroare la parsarea entitatii din fisier: " + e.getMessage());
        }
    }

    /**
     * Implementarea Template Method: Creeaza un rand de text dintr-o entitate User.
     */
    @Override
    protected String createEntityAsString(User entity) {
        // Folosim String.format pentru a simplifica serializarea (evita lantul de append-uri)
        switch (entity) {
            case Persoana p -> {
                return String.format("%d%sPersoana%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%d",
                        p.getId(), SEPARATOR, SEPARATOR,
                        p.getUsername(), SEPARATOR,
                        p.getEmail(), SEPARATOR,
                        p.getPasswordHash(), SEPARATOR,
                        p.getNume(), SEPARATOR,
                        p.getPrenume(), SEPARATOR,
                        p.getDataNasterii().toString(), SEPARATOR,
                        p.getOcupatie(), SEPARATOR,
                        p.getNivelEmpatie());
            }
            case SwimmingDuck d -> {
                // Include FlyingAndSwimmingDuck care are aceleasi atribute de baza
                String duckType;
                if (entity instanceof FlyingAndSwimmingDuck) {
                    duckType = "FlyingAndSwimming";
                } else {
                    duckType = "Swimming";
                }
                return String.format("%d%sDuck%s%s%s%s%s%s%s%s%s%.2f%s%.2f",
                        d.getId(), SEPARATOR, SEPARATOR,
                        d.getUsername(), SEPARATOR,
                        d.getEmail(), SEPARATOR,
                        d.getPasswordHash(), SEPARATOR,
                        duckType, SEPARATOR,
                        d.getViteza(), SEPARATOR,
                        d.getRezistenta());
            }
            case FlyingDuck d -> {
                return String.format("%d%sDuck%s%s%s%s%s%s%sFlying%s%.2f%s%.2f",
                        d.getId(), SEPARATOR, SEPARATOR,
                        d.getUsername(), SEPARATOR,
                        d.getEmail(), SEPARATOR,
                        d.getPasswordHash(), SEPARATOR,
                        SEPARATOR,
                        d.getViteza(), SEPARATOR,
                        d.getRezistenta());
            }
            case null, default ->
                // Arunca eroare daca incerci sa salvezi un tip de User necunoscut
                    throw new RepositoryException("Nu se poate serializa tipul de entitate necunoscut.");
        }
    }

    // --- Specificatii: Implementarea UserRepositoryInterface (cu Stream-uri) ---

    /**
     * Cauta si returneaza o lista de Rate (Duck) filtrate dupa un anumit tip.
     * Utilizeaza operatii pe stream-uri pentru filtrare si colectare.
     * @param userType Tipul de rata (clasa) dupa care se filtreaza (ex: "SwimmingDuck").
     * @return Lista de Rate care se potrivesc tipului.
     */
    @Override
    public List<Duck> findDucksByType(String userType){
        // Cazul "Toate" este gestionat in Service, dar returneaza toate ratele:
        if ("Toate".equalsIgnoreCase(userType) || userType == null || userType.trim().isEmpty()) {
            return findAllDucks();
        }

        // OPERATII PE STREAM-URI: Filtrare si mapare
        return entities.values().stream()
                .filter(user -> user instanceof Duck)
                .map(user -> (Duck) user)
                .filter(duck -> duck.getClass().getSimpleName().equalsIgnoreCase(userType))
                .collect(Collectors.toList());
    }

    /**
     * Cauta si returneaza toate Ratele (Duck) din depozit.
     * Utilizeaza operatii pe stream-uri pentru filtrare si colectare.
     * @return Lista care contine toate entitatile de tip Duck.
     */
    @Override
    public List<Duck> findAllDucks() {
        // OPERATII PE STREAM-URI: Filtrare si mapare
        return entities.values().stream()
                .filter(user -> user instanceof Duck)
                .map(user -> (Duck) user)
                .collect(Collectors.toList());
    }

    /**
     * Cauta un utilizator dupa username.
     * Utilizeaza operatii pe stream-uri pentru a gasi prima potrivire.
     * @param username Username-ul de cautat.
     * @return Optional<User> daca utilizatorul este gasit, Optional.empty() altfel.
     */
    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();

        // OPERATII PE STREAM-URI: Cautarea primei potriviri (inlocuieste un for)
        return entities.values().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    // --- Specificatii: Implementari simple (Moștenite) ---

    @Override
    public long count(){
        return size(); // size() este mostenita si implementata in AbstractFileRepository
    }

    // Paginare (Implementari minimale, conform cerintei originale)
    @Override
    public List<User> findAll(Pageable pageable) {
        return new ArrayList<>(); // Implementare minimala
    }

    @Override
    public Page<User> findUsersPaginatedAndFiltered(Pageable pageable, String filterType){
        return null; // Implementare minimala
    }

    @Override
    public void updateProfileImage(Long userId, String newImagePath){}
}