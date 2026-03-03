package com.ubb.repository;

import com.ubb.domain.Friendship;
import com.ubb.domain.Tuple;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.exceptions.RepositoryException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Repository specific pentru entitatile de tip Friendship, care persista datele in fisier.
 * Extinde AbstractFileRepository si implementeaza Template Methods pentru I/O.
 */
public class FriendshipFileRepository extends AbstractFileRepository<Tuple<Long, Long>, Friendship> {

    // Formatul standard pentru data si ora (pentru a fi consistent in fisier)
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String SEPARATOR = ";";

    /**
     * Constructor.
     * @param fileName Numele fisierului de persistenta.
     * @param validator Validatorul pentru entitati de tip Friendship.
     */
    public FriendshipFileRepository(String fileName, Validator<Friendship> validator) {
        super(fileName, validator);
    }

    /**
     * Implementarea Template Method: Extrage entitatea Friendship dintr-un rand de text.
     * Format asteptat: ID_User1;ID_User2;FriendsFrom(YYYY-MM-DD HH:MM:SS)
     *
     * @param line Linia de text din fisier.
     * @return Entitatea Friendship extrasa.
     */
    @Override
    protected Friendship extractEntity(String line) {
        try {
            String[] parts = line.split(SEPARATOR);
            if (parts.length < 3) {
                throw new RepositoryException("Linie incompleta sau format invalid pentru Friendship.");
            }

            // Extrage ID-urile (Long)
            Long idUser1 = Long.parseLong(parts[0].trim());
            Long idUser2 = Long.parseLong(parts[1].trim());

            // Extrage data (LocalDateTime)
            LocalDateTime friendsFrom = LocalDateTime.parse(parts[2].trim(), DATE_TIME_FORMATTER);

            // Creeaza entitatea folosind constructorul cu data (pentru a prelua data din fisier)
            // Constructorul Friendship se asigura ca ID-ul compus (Tuple) este standardizat (ID mic, ID mare).
            return new Friendship(idUser1, idUser2, friendsFrom);

        } catch (NumberFormatException | DateTimeParseException e) {
            throw new RepositoryException("Eroare la parsarea datelor (ID sau Data) pentru Friendship: " + e.getMessage());
        } catch (Exception e) {
            // Include orice alte exceptii, inclusiv cele din constructor (ex: ID-uri egale)
            throw new RepositoryException("Eroare la crearea entitatii Friendship din linia: " + line + ". Cauza: " + e.getMessage());
        }
    }

    /**
     * Implementarea Template Method: Creeaza un rand de text dintr-o entitate Friendship.
     * Format de scriere: ID_Mic;ID_Mare;FriendsFrom
     *
     * @param entity Entitatea Friendship de serializat.
     * @return String-ul serializat.
     */
    @Override
    protected String createEntityAsString(Friendship entity) {
        // Folosim String.format pentru concizie
        return String.format("%d%s%d%s%s",
                entity.getIdUser1(), SEPARATOR,
                entity.getIdUser2(), SEPARATOR,
                entity.getFriendsFrom().format(DATE_TIME_FORMATTER));
    }
}