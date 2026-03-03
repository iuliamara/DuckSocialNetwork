package com.ubb.gui;

import com.ubb.domain.*;
import com.ubb.service.FriendshipRequestServiceInterface;
import com.ubb.service.FriendshipServiceInterface;
import com.ubb.service.UserServiceInterface;
import com.ubb.service.exceptions.ServiceException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class NotificationController implements Observer<FriendshipRequest> {

    @FXML private ListView<FriendshipRequest> notificationListView;

    // Referinte catre butoanele din FXML pentru a evita NullPointerException la load,
    // chiar daca logica principala este mutata in ProfileController.
    @FXML private Button acceptButton;
    @FXML private Button rejectButton;

    private FriendshipRequestServiceInterface requestService;
    private UserServiceInterface userService;
    private FriendshipServiceInterface friendshipService;
    private User authenticatedUser;

    private final ObservableList<FriendshipRequest> notificationData = FXCollections.observableArrayList();

    /**
     * Injecteaza toate serviciile necesare pentru a le putea pasa mai departe catre ProfileController.
     */
    public void setContext(FriendshipRequestServiceInterface requestService,
                           UserServiceInterface userService,
                           FriendshipServiceInterface friendshipService,
                           User authenticatedUser) {
        this.requestService = requestService;
        this.userService = userService;
        this.friendshipService = friendshipService;
        this.authenticatedUser = authenticatedUser;

        // Inscriere la update-uri pentru badge-uri si lista in timp real
        if (requestService instanceof Subject) {
            ((Subject<FriendshipRequest>) requestService).addObserver(this);
        }

        if (notificationListView != null) {
            notificationListView.setCellFactory(listView -> new NotificationCell(authenticatedUser));
        }

        loadNotifications();
    }

    public void unsubscribe() {
        if (requestService instanceof Subject) {
            ((Subject<FriendshipRequest>) requestService).removeObserver(this);
        }
    }

    @FXML
    public void initialize() {
        notificationListView.setItems(notificationData);
        notificationListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // Declanseaza vizualizarea profilului la click pe o notificare
        notificationListView.setOnMouseClicked(event -> {
            FriendshipRequest selected = notificationListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleNotificationClick(selected);
            }
        });
    }

    private void loadNotifications() {
        if (requestService == null || authenticatedUser == null) return;
        try {
            // Preluam toate notificarile (PENDING, APPROVED, REJECTED) primite de utilizator
            List<FriendshipRequest> requests = requestService.getNotifications(authenticatedUser.getId());
            Platform.runLater(() -> notificationData.setAll(requests));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleNotificationClick(FriendshipRequest req) {
        // Verificam daca utilizatorul curent este destinatarul cererii
        boolean isReceived = req.getToUser().getId().equals(authenticatedUser.getId());
        boolean isPending = req.getStatus() == RequestStatus.PENDING;

        // 1. Marcare ca citit pentru a actualiza badge-ul si stilul vizual
        if (isReceived && !req.isViewedByRecipient()) {
            try {
                requestService.markAsViewed(req.getId());
            } catch (ServiceException e) {
                e.printStackTrace();
            }
        }

        // 2. Deschiderea ferestrei de Profil pentru a permite decizia (Accept/Reject)
        // Se deschide doar daca cererea este inca in asteptare (PENDING)
        if (isReceived && isPending) {
            showProfileView(req.getFromUser());
        }

        Platform.runLater(() -> notificationListView.getSelectionModel().clearSelection());
    }

    @Override
    public void update(Subject<FriendshipRequest> subject, FriendshipRequest request) {
        // Reincarcare automata a listei la orice schimbare detectata de Observer
        Platform.runLater(this::loadNotifications);
    }

    /**
     * Deschide ProfileController pentru a permite decizia informata dupa vizualizarea detaliilor.
     */
    private void showProfileView(User sender) {
        try {
            // Folosim calea catre profile.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProfileView.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));

            ProfileController controller = loader.getController();

            // Pasam toate serviciile; ProfileController va detecta automat cererea PENDING
            controller.setContext(userService, friendshipService, requestService, authenticatedUser, sender);

            stage.setTitle("Profilul lui " + sender.getUsername());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(notificationListView.getScene().getWindow());

            // Folosim showAndWait pentru a suspenda executia pana cand utilizatorul ia o decizie
            stage.showAndWait();

        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Eroare la deschiderea profilului: " + e.getMessage()).show();
        }
    }

    // --- CUSTOM LIST CELL ---

    private static class NotificationCell extends ListCell<FriendshipRequest> {
        private final User authUser;

        public NotificationCell(User authUser) {
            this.authUser = authUser;
        }

        @Override
        protected void updateItem(FriendshipRequest item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setStyle(null);
            } else {
                boolean isReceived = item.getToUser().getId().equals(authUser.getId());
                String type = isReceived ? "PRIMITI" : "TRIMISI";
                String otherUser = isReceived ? item.getFromUser().getUsername() : item.getToUser().getUsername();

                setText(String.format("[%s] %s -> Stare: %s", type, otherUser, item.getStatus()));

                // Stil BOLD si fundal portocaliu/galben pentru cererile noi primite si nevizualizate
                if (isReceived && !item.isViewedByRecipient()) {
                    setStyle("-fx-background-color: #ffe0b2; -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 5;");
                } else {
                    setStyle("-fx-padding: 10; -fx-background-color: transparent;");
                }
            }
        }
    }
}