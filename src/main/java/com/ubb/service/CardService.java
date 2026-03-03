package com.ubb.service;

import com.ubb.domain.Card;
import com.ubb.domain.Duck;
import com.ubb.domain.User;
import com.ubb.domain.SwimmingCard;
import com.ubb.domain.SwimmingDuck;
import com.ubb.domain.FlyingCard;
import com.ubb.domain.FlyingDuck;
import com.ubb.domain.validation.Validator;
import com.ubb.repository.CardRepositoryInterface;
import com.ubb.repository.Repository;
import com.ubb.service.exceptions.EntityNotFoundException;
import com.ubb.service.exceptions.ServiceException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Serviciu care gestioneaza logica de business pentru entitatile Card.
 * Include logica de reconstructie a relatiei M:N (Card <-> Duck) si calculul performantei.
 */
public class CardService implements CardServiceInterface {

    // Injectam Repository-ul de Card-uri
    private final CardRepositoryInterface cardRepository;

    // Injectam Repository-ul de Useri/Rate (necesar pentru a gasi membrii reali)
    private final Repository<Long, User> userRepository;
    private final Validator<Card<? extends Duck>> cardValidator;

    /**
     * Constructor cu Dependency Injection.
     */
    public CardService(Repository<Long, Card<? extends Duck>> cardRepository,
                       Repository<Long, User> userRepository,
                       Validator<Card<? extends Duck>> cardValidator) {

        this.cardRepository = (CardRepositoryInterface) cardRepository;
        this.userRepository = userRepository;
        this.cardValidator = cardValidator;
    }

    // --- METODE AUXILIARE (cu Stream-uri) ---

    /**
     * Metoda helper care populeaza obiectul Card cu obiectele Duck reale (Membrii).
     * Ruleaza la incarcarea unui Card specific.
     */
    private Card<? extends Duck> reconstructMembers(Card<? extends Duck> card) {
        card.clearMembri();

        List<Long> memberIds = cardRepository.getMemberIdsForCard(card.getId());

        if (memberIds == null || memberIds.isEmpty()) {
            return card;
        }

        // OPERATIE PE STREAM-URI: Gaseste User-ii, filtreaza Ducks si ii adauga
        memberIds.stream()
                .map(userRepository::findOne)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(user -> user instanceof Duck)
                .forEach(user -> {
                    // Cast nesigur, dar necesar pentru a adauga in lista generica.
                    @SuppressWarnings("unchecked")
                    Card<Duck> uncheckedCard = (Card<Duck>) card;
                    uncheckedCard.populateMembru((Duck) user);
                });

        return card;
    }

    /**
     * Metoda helper: Extrage un Card din Repository si se asigura ca lista de membri este reconstruita.
     */
    private Card<? extends Duck> findCardById(Long id) {
        Optional<Card<? extends Duck>> cardOpt = cardRepository.findOne(id);

        if (cardOpt.isEmpty()) {
            throw new EntityNotFoundException("Cardul cu ID-ul " + id + " nu a fost gasit.");
        }

        // Apelam reconstructia membrilor (folosind datele din tabelele auxiliare/fisier)
        return reconstructMembers(cardOpt.get());
    }

    // --- METODE CRUD si BUSINESS ---

    @Override
    public void addCard(Card<? extends Duck> card) {
        if (card == null) throw new IllegalArgumentException("Cardul nu poate fi null.");

        cardValidator.validate(card);
        cardRepository.save(card);
    }

    /**
     * Adauga un membru (o rata) intr-un card existent.
     * Include verificari de existenta si compatibilitate de tip.
     */
    @Override
    public void addMembru(Long cardId, Long duckId) {
        // Gaseste Cardul si reconstruieste-i membrii
        Card<? extends Duck> card = findCardById(cardId);

        Optional<User> userOpt = userRepository.findOne(duckId);

        // 1. Verifica daca rata exista si este de tip Duck
        if (userOpt.isEmpty() || !(userOpt.get() instanceof Duck)) {
            throw new EntityNotFoundException("Rata cu ID-ul " + duckId + " nu exista sau nu este o rata.");
        }

        Duck duck = (Duck) userOpt.get();

        // 2. Logica de Business: Verifica compatibilitatea tipului de rata cu tipul de card
        if (card instanceof SwimmingCard && !(duck instanceof SwimmingDuck)) {
            throw new ServiceException("Eroare: Nu se poate adauga un " + duck.getClass().getSimpleName() +
                    " intr-un SwimmingCard.");
        }
        if (card instanceof FlyingCard && !(duck instanceof FlyingDuck)) {
            throw new ServiceException("Eroare: Nu se poate adauga un " + duck.getClass().getSimpleName() +
                    " intr-un FlyingCard.");
        }

        // 3. Adauga membrul (AdMembru face si verificarea de unicitate)
        try {
            @SuppressWarnings("unchecked")
            Card<Duck> uncheckedCard = (Card<Duck>) card;
            uncheckedCard.addMembru(duck);
        } catch (ClassCastException e) {
            throw new ServiceException("Eroare la adaugarea membrului in Card.", e);
        }

        // 4. Salveaza Cardul modificat (re-scrie in fisier/BD)
        cardRepository.save(card);
    }

    /**
     * Returneaza lista de membri (Duck) a unui Card specific.
     */
    @Override
    public List<? extends Duck> getCardMembers(Long cardId) {
        // Gaseste Cardul si il reconstruieste (populeaza lista de membri)
        Card<? extends Duck> card = findCardById(cardId);

        // Returneaza lista populata
        return card.getMembri();
    }

    /**
     * Calculeaza si returneaza performanta medie a unui card.
     */
    @Override
    public double getPerformantaMedie(Long cardId) {
        // Gaseste cardul si reconstruieste-l inainte de a calcula performanta
        Card<? extends Duck> card = cardRepository.findOne(cardId).orElseThrow(
                () -> new EntityNotFoundException("Cardul nu a fost gasit.")
        );
        // Reconstructia trebuie facuta pe card inainte de a apela logica de domeniu (getPerformantaMedie)
        card = reconstructMembers(card);

        return card.getPerformantaMedie();
    }

    /**
     * Returneaza toate cardurile, asigurand ca fiecare card este reconstruit cu membrii sai.
     */
    @Override
    public Iterable<Card<? extends Duck>> findAllCards() {
        Iterable<Card<? extends Duck>> rawCards = cardRepository.findAll();

        // OPERATIE PE STREAM-URI: Itereaza peste Cardurile 'goale' si le reconstruieste pe fiecare
        List<Card<? extends Duck>> allCards = StreamSupport.stream(rawCards.spliterator(), false)
                .map(rawCard -> findCardById(rawCard.getId()))
                .collect(Collectors.toList());

        return allCards;
    }
}