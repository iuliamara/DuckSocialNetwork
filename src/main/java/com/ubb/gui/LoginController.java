package com.ubb.gui;

import com.ubb.MainFX;
import com.ubb.domain.User;
import com.ubb.service.*;
import com.ubb.service.exceptions.AuthenticationException;
import com.ubb.service.exceptions.ServiceException;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Controller-ul pentru fereastra de Login.
 * Acesta primeste toate serviciile de la Launcher si le paseaza mai departe catre MainFX la logare.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    // --- DEPENDENTE ---
    private Stage dialogStage;
    private MainFX mainApp;

    // --- SERVICII (Le pastram pentru a le pasa sesiunii urmatoare) ---
    private UserServiceInterface userService;
    private FriendshipServiceInterface friendshipService;
    private NetworkServiceInterface networkService;
    private MessageServiceInterface messageService;
    private FriendshipRequestServiceInterface requestService;

    /**
     * Seteaza toate serviciile necesare pentru aplicatie.
     * Acestea vin din Launcher si vor fi trimise catre MainController dupa login.
     */
    public void setServices(UserServiceInterface userService,
                            FriendshipServiceInterface friendshipService,
                            NetworkServiceInterface networkService,
                            MessageServiceInterface messageService,
                            FriendshipRequestServiceInterface requestService,
                            Stage dialogStage,
                            MainFX mainApp) {
        this.userService = userService;
        this.friendshipService = friendshipService;
        this.networkService = networkService;
        this.messageService = messageService;
        this.requestService = requestService;

        this.dialogStage = dialogStage;
        this.mainApp = mainApp;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Introduceti username si parola.");
            return;
        }

        try {
            // 1. Verificam credientialele
            User user = userService.login(username, password);

            if (user != null) {
                // 2. Logare reusita -> Cerem MainFX sa deschida fereastra principala
                // Pasam User-ul logat + toate serviciile necesare in MainController
                mainApp.openNewApplicationForUser(
                        user,
                        userService,
                        friendshipService,
                        networkService,
                        messageService,
                        requestService
                );

                // 3. Inchidem fereastra de login
                dialogStage.close();
            }

        } catch (AuthenticationException e) {
            messageLabel.setText("Date incorecte: " + e.getMessage());
        } catch (ServiceException e) {
            messageLabel.setText("Eroare de sistem: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Eroare neasteptata.");
        }
    }
}