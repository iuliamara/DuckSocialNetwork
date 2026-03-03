package com.ubb;

import com.ubb.domain.*;
import com.ubb.domain.exceptions.ValidationException;
import com.ubb.domain.validation.*;
import com.ubb.repository.*;
import com.ubb.service.*;
import com.ubb.ui.Console;

public class Main {

    // Numele fisierelor de persistenta
    //private static final String USERS_FILE = "src/data/users.csv";
    //private static final String FRIENDSHIPS_FILE = "src/data/friendships.csv";
    private static final String CARDS_FILE = "src/data/cards.csv";
    private static final String EVENTS_FILE = "src/data/events.csv";

    // Date de conexiune PostgreSQL
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/ducksocialnetworkdb";
    private static final String DB_USER = "postgres"; // <-- MODIFICa AICI
    private static final String DB_PASSWORD = "mara2005"; // <-- MODIFICa AICI

    public static void main(String[] args) {

        //DOMAIN: INITIALIZARE VALIDATORI
        Validator<Persoana> persoanaValidator = new PersoanaValidator();
        Validator<SwimmingDuck> swimmingDuckValidator = new SwimmingDuckValidator();
        Validator<FlyingDuck> flyingDuckValidator = new FlyingDuckValidator();
        Validator<FlyingAndSwimmingDuck> flyingAndSwimmingDuckValidator = new FlyingAndSwimmingDuckValidator();
        FriendshipValidator friendshipValidator = new FriendshipValidator();
        @SuppressWarnings("unchecked")
        Validator<Card<? extends Duck>> cardValidator =
                (Validator<Card<? extends Duck>>) (Validator<?>) new CardValidator<Duck>();
        Validator<Event> eventValidator = new EventValidator();

        //REPOSITORY: INJECTARE VALIDATORI SI CALEA FISIERELOR
        // Wrapper-ul care satisface cerinta AbstractFileRepository de a primi un Validator<User>
        Validator<User> userWrapperValidator = new Validator<User>() {
            @Override
            public void validate(User user) throws ValidationException {
                // Redirectioneaza validarea
                if (user instanceof Persoana) {
                    persoanaValidator.validate((Persoana) user);
                } else if (user instanceof SwimmingDuck) { // NOU
                    swimmingDuckValidator.validate((SwimmingDuck) user);
                } else if (user instanceof FlyingDuck) { // NOU
                    flyingDuckValidator.validate((FlyingDuck) user);
                } else if (user instanceof FlyingAndSwimmingDuck) {
                    flyingAndSwimmingDuckValidator.validate((FlyingAndSwimmingDuck) user);
                }
                else {
                    throw new ValidationException("Tip de utilizator necunoscut pentru validare.");
                }
            }
        };
//        Repository<Long, User> userRepository =
//                new UserFileRepository(USERS_FILE, userWrapperValidator);

        UserRepositoryInterface userRepository =
                new UserDBRepository(DB_URL, DB_USER, DB_PASSWORD, userWrapperValidator);

//        Repository<Tuple<Long, Long>, Friendship> friendshipRepository =
//                new FriendshipFileRepository(FRIENDSHIPS_FILE, friendshipValidator);

        FriendshipRepositoryInterface friendshipRepository =
                new FriendshipDBRepository(DB_URL, DB_USER, DB_PASSWORD, friendshipValidator);

//        Repository<Long, Card<? extends Duck>> cardRepository =
//              new CardFileRepository(CARDS_FILE, cardValidator);
        Repository<Long, Card<? extends Duck>> cardRepository =
                new CardDBRepository(DB_URL, DB_USER, DB_PASSWORD, cardValidator);

//        Repository<Long, Event> eventRepository =
//                new EventFileRepository(EVENTS_FILE, eventValidator);
        Repository<Long, Event> eventRepository =
                new EventDBRepository(DB_URL, DB_USER, DB_PASSWORD, eventValidator);

        //SERVICE: INJECTARE REPOSITORII SI VALIDATORI ---

        //UserService depinde doar de UserRepository
        UserServiceInterface userService =
                new UserService(userRepository, persoanaValidator, swimmingDuckValidator, flyingDuckValidator,flyingAndSwimmingDuckValidator);

        //FriendshipService depinde de ambele Repositorii
        FriendshipServiceInterface friendshipService =
                new FriendshipService(friendshipRepository,userService, userRepository, friendshipValidator);

        //NetworkService depinde de ambele Repositorii
        NetworkServiceInterface networkService =
                new NetworkService(userRepository, friendshipRepository);

        CardServiceInterface cardService = new CardService(cardRepository,userRepository,cardValidator);

        EventServiceInterface eventService =
                new EventService(eventRepository, userRepository, eventValidator);

        //UI/CONSOLE: INJECTARE SERVICII

        Console console = new Console(userService, friendshipService, networkService,cardService, eventService);

        System.out.println("Social Network Application Started.");

        //RUN APPLICATION
        console.run();
    }
}