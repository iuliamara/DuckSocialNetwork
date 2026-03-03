// com/ubb/repository/AbstractFileRepository.java
package com.ubb.repository;

import com.ubb.domain.Entity;
import com.ubb.domain.exceptions.ValidationException;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.exceptions.RepositoryException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Clasa abstracta care implementeaza Repository si Template Method Pattern.
 * Gestioneaza citirea/scrierea din fisier.
 *
 * @param <ID> Tipul ID-ului
 * @param <E> Tipul Entitatii
 */
public abstract class AbstractFileRepository<ID, E extends Entity<ID>> implements Repository<ID, E> {

    protected Map<ID, E> entities = new HashMap<>();
    protected final Validator<E> validator;
    private final String fileName;

    // Harta auxiliara pentru a stoca date aditionale (ex: membrii unui Card) in timpul incarcarii
    protected final Map<ID, List<Long>> auxiliaryMemberMap = new HashMap<>();

    /**
     * Constructor. Incarca datele din fisier la initializare.
     */
    public AbstractFileRepository(String fileName, Validator<E> validator) {
        this.fileName = fileName;
        this.validator = validator;
        loadData(); // Incarca datele la initializare (Template Method Pattern)
    }

    // --- Specificatii: Metode Auxiliare Interne ---

    /**
     * Metoda protected care permite claselor copil sa stocheze date auxiliare la incarcare.
     * @param key ID-ul entitatii parinte (ex: ID-ul Cardului).
     * @param memberIds Lista de ID-uri auxiliare (ex: ID-urile membrilor).
     */
    protected void putAuxiliaryData(ID key, List<Long> memberIds) {
        auxiliaryMemberMap.put(key, memberIds);
    }

    /**
     * Metoda helper pentru a face casting-ul ID-ului (ex: Long la ID generic).
     */
    @SuppressWarnings("unchecked")
    protected ID castToID(Long longId) {
        // Casteaza Long-ul la tipul generic ID
        return (ID) longId;
    }

    // --- Specificatii: Metoda Publica pentru Service (Mentainuta) ---

    /**
     * Returneaza ID-urile membrilor pentru un Card specificat (utila in Service).
     * @param cardId ID-ul Cardului.
     * @return Lista de ID-uri Long (ID-urile membrilor), sau lista goala.
     */
    public List<Long> getMemberIdsForCard(Long cardId) {
        // Trebuie sa facem cast deoarece auxiliaryMemberMap foloseste ID generic.
        return auxiliaryMemberMap.getOrDefault(castToID(cardId), Collections.emptyList());
    }

    // --- Specificatii: Template Methods ---

    /**
     * Hook method: Metoda apelata inainte de salvare, oferind claselor copil
     * posibilitatea de a modifica entitatea (ex: generarea ID-ului).
     * @param entity Entitatea care urmeaza sa fie salvata.
     */
    protected E preSaveHook(E entity) {
        // Implementarea implicita este returnarea entitatii nemodificate.
        return entity;
    }

    /**
     * Template Method 1: Extrage o entitate dintr-un rand de text.
     * Trebuie implementata de clasele copil (UserFileRepository, FriendshipFileRepository).
     * @param line Linia de text din fisier.
     * @return Entitatea extrasa.
     */
    protected abstract E extractEntity(String line);

    /**
     * Template Method 2: Creeaza un rand de text dintr-o entitate.
     * Trebuie implementata de clasele copil.
     * @param entity Entitatea de serializat.
     * @return String-ul serializat.
     */
    protected abstract String createEntityAsString(E entity);

    // --- Specificatii: Logica I/O (cu Stream-uri) ---

    /**
     * Incarca datele din fisierul specificat folosind operatii pe stream-uri.
     */
    protected void loadData() {
        Path path = Paths.get(fileName);
        if (!Files.exists(path)) {
            System.err.println("Fisierul " + fileName + " nu a fost gasit. Se va crea unul nou la salvare.");
            return;
        }

        try (Stream<String> stream = Files.lines(path)) {
            // OPERATII PE STREAM-URI: Citirea si procesarea liniilor
            stream.filter(line -> !line.isEmpty())
                    .forEach(line -> {
                        try {
                            E entity = extractEntity(line);
                            // Folosim put direct in map pentru a evita rularea validatorului la incarcare
                            this.entities.put(entity.getId(), entity);
                        } catch (Exception e) {
                            System.err.println("Eroare la citirea liniei: " + line + " -> " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            throw new RepositoryException("Eroare I/O la incarcarea datelor din fisier: " + e.getMessage());
        }
    }

    /**
     * Salveaza toate entitatile inapoi in fisier folosind operatii pe stream-uri.
     */
    protected void writeToFile() {
        // Colectam toate string-urile entitatilor
        List<String> lines = entities.values().stream()
                .map(this::createEntityAsString)
                .collect(Collectors.toList());

        try {
            // OPERATII PE STREAM-URI: Scrierea liniilor in fisier
            Files.write(Paths.get(fileName), lines);
        } catch (IOException e) {
            throw new RepositoryException("Eroare I/O la salvarea datelor in fisier: " + e.getMessage());
        }
    }

    // --- Specificatii: Implementarea metodelor Repository (CRUD) ---

    @Override
    public Optional<E> save(E entity) throws RepositoryException {
        if (entity == null) throw new IllegalArgumentException("Entitatea nu poate fi nula!");

        // 1. Hook (Template Method)
        entity = preSaveHook(entity);

        if (entity.getId() == null) {
            throw new RepositoryException("Eroare interna: ID-ul nu a putut fi generat de clasa copil.");
        }

        // 2. Validare (Strategy Pattern)
        try {
            validator.validate(entity);
        } catch (ValidationException e) {
            throw new RepositoryException("Date entitate invalide: " + e.getMessage());
        }

        // 3. Logica de salvare
        Optional<E> result = Optional.empty();
        if (entities.containsKey(entity.getId())) {
            result = Optional.of(entities.get(entity.getId())); // Returneaza vechea entitate
        }

        entities.put(entity.getId(), entity);

        // 4. Persistenta (Template Method)
        writeToFile();

        return result;
    }

    @Override
    public Optional<E> delete(ID id) throws IllegalArgumentException {
        if (id == null) throw new IllegalArgumentException("ID-ul nu poate fi nul!");

        Optional<E> deleted = Optional.ofNullable(entities.remove(id));

        if (deleted.isPresent()) {
            writeToFile(); // Salveaza modificarea in fisier
        }
        return deleted;
    }

    @Override
    public Optional<E> findOne(ID id) { return Optional.ofNullable(entities.get(id)); }

    @Override
    public Iterable<E> findAll() { return entities.values(); }

    @Override
    public long size() { return entities.size(); }
}