package com.ubb;

import com.ubb.domain.*;
import com.ubb.domain.exceptions.ValidationException;
import com.ubb.domain.validation.*;
import com.ubb.gui.LauncherController;
import com.ubb.gui.MainController;
import com.ubb.repository.*;
import com.ubb.service.*;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFX extends Application {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/ducksocialnetworkdb";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "mara2005";

    private UserServiceInterface userService;
    private FriendshipServiceInterface friendshipService;
    private NetworkServiceInterface networkService;
    private MessageServiceInterface messageService;
    private FriendshipRequestServiceInterface friendshipRequestService;
    private EventServiceInterface eventService; // NOU (Lab 10)

    private Stage primaryStage;

    @Override
    public void init() throws Exception {
        // 1. VALIDATORI
        Validator<Persoana> persoanaValidator = new PersoanaValidator();
        Validator<SwimmingDuck> swimmingDuckValidator = new SwimmingDuckValidator();
        Validator<FlyingDuck> flyingDuckValidator = new FlyingDuckValidator();
        Validator<FlyingAndSwimmingDuck> flyingAndSwimmingDuckValidator = new FlyingAndSwimmingDuckValidator();

        FriendshipValidator friendshipValidator = new FriendshipValidator();
        MessageValidator messageValidator = new MessageValidator();
        FriendshipRequestValidator friendshipRequestValidator = new FriendshipRequestValidator();

        // Validator pentru Evenimente
        Validator<Event> eventValidator = event -> {
            if (event.getName() == null || event.getName().isEmpty())
                throw new ValidationException("Numele evenimentului este obligatoriu.");
        };

        Validator<User> userWrapperValidator = user -> {
            if (user instanceof Persoana) persoanaValidator.validate((Persoana) user);
            else if (user instanceof SwimmingDuck) swimmingDuckValidator.validate((SwimmingDuck) user);
            else if (user instanceof FlyingDuck) flyingDuckValidator.validate((FlyingDuck) user);
            else if (user instanceof FlyingAndSwimmingDuck) flyingAndSwimmingDuckValidator.validate((FlyingAndSwimmingDuck) user);
            else throw new ValidationException("Tip de utilizator necunoscut.");
        };

        // 2. REPOSITORIES
        UserRepositoryInterface userRepository = new UserDBRepository(DB_URL, DB_USER, DB_PASSWORD, userWrapperValidator);
        FriendshipRepositoryInterface friendshipRepository = new FriendshipDBRepository(DB_URL, DB_USER, DB_PASSWORD, friendshipValidator);
        MessageRepositoryInterface messageRepository = new MessageDBRepository(DB_URL, DB_USER, DB_PASSWORD, messageValidator, userRepository);
        FriendshipRequestRepositoryInterface friendshipRequestRepository = new FriendshipRequestsDBRepository(DB_URL, DB_USER, DB_PASSWORD, friendshipRequestValidator, userRepository);

        // EventDBRepository (M:N persistenta)
        EventRepositoryInterface eventRepository = new EventDBRepository(DB_URL, DB_USER, DB_PASSWORD, eventValidator);

        // 3. SERVICES
        this.userService = new UserService(userRepository, persoanaValidator, swimmingDuckValidator, flyingDuckValidator, flyingAndSwimmingDuckValidator);
        this.friendshipService = new FriendshipService(friendshipRepository, userService, userRepository, friendshipValidator);
        this.networkService = new NetworkService(userRepository, friendshipRepository);
        this.messageService = new MessageService(messageRepository, userRepository, messageValidator);
        this.friendshipRequestService = new FriendshipRequestService(friendshipRequestRepository, userRepository, friendshipRepository, friendshipRequestValidator);

        // EventService (Asincron logic inclus)
        this.eventService = new EventService(eventRepository, userRepository, eventValidator);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Social Network - Launcher");
        showLauncherView();
    }

    private void showLauncherView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LauncherWelcomeView.fxml"));
        Parent root = loader.load();
        LauncherController controller = loader.getController();

        controller.setServices(this, userService, friendshipService, networkService, messageService, friendshipRequestService,eventService);

        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public void openNewApplicationForUser(User authenticatedUser,
                                          UserServiceInterface userService,
                                          FriendshipServiceInterface friendshipService,
                                          NetworkServiceInterface networkService,
                                          MessageServiceInterface messageService,
                                          FriendshipRequestServiceInterface requestService) {
        try {
            Stage userStage = new Stage();
            userStage.setTitle("Social Network - " + authenticatedUser.getUsername());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainView.fxml"));
            Parent root = loader.load();

            MainController controller = loader.getController();

            controller.setContext(
                    authenticatedUser,
                    userService,
                    friendshipService,
                    networkService,
                    messageService,
                    requestService,
                    this.eventService // NOU
            );

            userStage.setOnCloseRequest(event -> {
                if (this.eventService != null) this.eventService.shutdown();
            });

            userStage.setScene(new Scene(root, 900, 650));
            userStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}