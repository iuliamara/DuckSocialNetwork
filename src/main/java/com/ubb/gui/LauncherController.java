package com.ubb.gui;

import com.ubb.MainFX;
import com.ubb.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class LauncherController {

    private MainFX mainApp;

    // --- SERVICIILE ---
    private UserServiceInterface userService;
    private FriendshipServiceInterface friendshipService;
    private NetworkServiceInterface networkService;
    private MessageServiceInterface messageService;
    private FriendshipRequestServiceInterface requestService;
    private EventServiceInterface eventService;

    /**
     * Injectarea dependentelor (Apelata din MainFX la start).
     * Primim TOATE serviciile aici pentru a le putea pasa mai departe.
     */
    public void setServices(MainFX mainApp,
                            UserServiceInterface userService,
                            FriendshipServiceInterface friendshipService,
                            NetworkServiceInterface networkService,
                            MessageServiceInterface messageService,
                            FriendshipRequestServiceInterface requestService,
                            EventServiceInterface eventService) {
        this.mainApp = mainApp;
        this.userService = userService;
        this.friendshipService = friendshipService;
        this.networkService = networkService;
        this.messageService = messageService;
        this.requestService = requestService;
        this.eventService = eventService;
    }

    // ========================================================================
    // CALEA 1: LOGARE UTILIZATOR (User Normal)
    // ========================================================================
    @FXML
    private void handleLaunchUserSession() {
        if (userService == null) {
            showError("Eroare critica: Serviciile nu sunt initializate!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
            Pane loginRoot = loader.load();

            LoginController loginController = loader.getController();

            Stage loginStage = new Stage();
            loginStage.setTitle("Autentificare SocialNetwork");

            // Pasam serviciile catre LoginController.
            // Acesta va avea nevoie de ele pentru a configura MainController dupa login.
            loginController.setServices(
                    userService,
                    friendshipService,
                    networkService,
                    messageService,
                    requestService,
                    loginStage,
                    mainApp
            );

            loginStage.setScene(new Scene(loginRoot));
            loginStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Nu s-a putut deschide fereastra de Login: " + e.getMessage());
        }
    }

    // ========================================================================
    // CALEA 2: LOGARE ADMINISTRATOR (Parola -> Admin Panel)
    // ========================================================================
    @FXML
    private void handleLaunchAdminSession() {
        if (userService == null || friendshipService == null || networkService == null || messageService == null || requestService == null || eventService == null) {
            showError("Eroare critica: Serviciile necesare Adminului nu sunt initializate!");
            return;
        }

        // 1. Crearea unui camp de parola blurat (PasswordField)
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Parola");

        // 2. Construirea layout-ului pentru dialog
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.getChildren().addAll(new javafx.scene.control.Label("Introduceti parola de administrator:"), passwordField);
        content.setPadding(new javafx.geometry.Insets(10));

        // 3. Configurarea Alertei (Dialogului) de tip CONFIRMATION
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Admin Access");
        alert.setHeaderText("Acces Restrictionat");
        alert.getDialogPane().setContent(content);

        // 4. Procesarea rezultatului
        Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            // Obtinem textul din PasswordField (care a fost blurat la tastare)
            String inputPassword = passwordField.getText();

            if (inputPassword.equals("admin123")) {
                openAdminPanel();
            } else {
                showError("Parola incorecta! Acces refuzat.");
            }
        }
    }

    private void openAdminPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AdminView.fxml"));
            Pane adminRoot = loader.load();

            // Obțtnem AdminController
            com.ubb.gui.AdminController adminController = loader.getController();

            // --- Injectam serviciile individual ---
            // Adminul are nevoie doar de User și Friendship services pentru CRUD
            adminController.setServices(this.userService, this.friendshipService , networkService,eventService);

            Stage adminStage = new Stage();
            adminStage.setTitle("Panou Administrator");
            adminStage.setScene(new Scene(adminRoot));
            adminStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Eroare la deschiderea panoului Admin: " + e.getMessage());
        }
    }

    // --- Helper pentru alerte ---
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Eroare");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}