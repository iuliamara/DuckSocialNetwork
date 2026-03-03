package com.ubb.gui;

import com.ubb.domain.User;
import com.ubb.service.UserServiceInterface;
import com.ubb.service.FriendshipServiceInterface;
import com.ubb.service.FriendshipRequestServiceInterface;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

public class SearchController {
    @FXML private TextField txtSearch;
    @FXML private ListView<User> usersListView;

    private UserServiceInterface userService;
    private FriendshipServiceInterface friendshipService;
    private FriendshipRequestServiceInterface requestService;
    private User authenticatedUser;

    // Lista care contine toti utilizatorii din sistem
    private final ObservableList<User> allUsers = FXCollections.observableArrayList();

    public void setContext(UserServiceInterface us, FriendshipServiceInterface fs,
                           FriendshipRequestServiceInterface rs, User authUser) {
        this.userService = us;
        this.friendshipService = fs;
        this.requestService = rs;
        this.authenticatedUser = authUser;

        // Datele se incarca abia dupa ce serviciile au fost injectate
        loadUsers();
    }

    @FXML
    public void initialize() {
        // 1. Initializam lista filtrata bazata pe allUsers
        FilteredList<User> filteredData = new FilteredList<>(allUsers, p -> true);

        // 2. Legam ListView la datele filtrate
        usersListView.setItems(filteredData);

        // 3. Logica de filtrare in timp real pe masura ce utilizatorul scrie
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return user.getUsername().toLowerCase().contains(lowerCaseFilter);
            });
        });

        // 4. Personalizarea aspectului celulelor (Username + Tip)
        usersListView.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String userType = item.getClass().getSimpleName();
                    setText(item.getUsername() + " (" + userType + ")");
                }
            }
        });

        // 5. Dublu click pentru a deschide profilul direct
        usersListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) handleShowProfile();
        });
    }

    private void loadUsers() {
        if (userService == null || authenticatedUser == null) return;

        try {
            // Conversie de la Iterable la List folosind StreamSupport
            List<User> users = StreamSupport.stream(userService.getAll().spliterator(), false)
                    .filter(u -> !u.getId().equals(authenticatedUser.getId()))
                    .toList();

            // Actualizam lista pe firul de executie principal al UI
            Platform.runLater(() -> allUsers.setAll(users));
        } catch (Exception e) {
            System.err.println("Eroare la incarcarea utilizatorilor in SearchController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleShowProfile() {
        User selected = usersListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProfileView.fxml"));
                Stage stage = new Stage();
                stage.setScene(new Scene(loader.load()));

                ProfileController ctrl = loader.getController();
                // Injectam serviciile si utilizatorii necesari pentru vizualizarea profilului
                ctrl.setContext(userService, friendshipService, requestService, authenticatedUser, selected);

                stage.setTitle("Profil: " + selected.getUsername());
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.show();
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Eroare la deschiderea profilului: " + e.getMessage()).show();
            }
        }
    }
}