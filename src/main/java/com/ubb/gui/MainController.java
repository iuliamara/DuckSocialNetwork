package com.ubb.gui;

import com.ubb.domain.*;
import com.ubb.service.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import java.io.IOException;
import java.net.URL;

/**
 * Controllerul principal care gestioneaza meniul lateral si navigarea.
 * Implementeaza Observer<Object> pentru a asculta mesaje, cereri si evenimente.
 */
public class MainController implements Observer<Object> {

    @FXML private BorderPane mainLayout;
    @FXML private Label lblWelcome;
    @FXML private Label lblNotifBadge;
    @FXML private Label lblChatBadge;
    @FXML private Button btnProfile, btnChat, btnSearch, btnNotifications, btnRaces;

    private User currentUser;
    private UserServiceInterface userService;
    private FriendshipServiceInterface friendshipService;
    private FriendshipRequestServiceInterface requestService;
    private NetworkServiceInterface networkService;
    private MessageServiceInterface messageService;
    private EventServiceInterface eventService;

    public void setContext(User user,
                           UserServiceInterface us,
                           FriendshipServiceInterface fs,
                           NetworkServiceInterface ns,
                           MessageServiceInterface ms,
                           FriendshipRequestServiceInterface rs,
                           EventServiceInterface es) {
        this.currentUser = user;
        this.userService = us;
        this.friendshipService = fs;
        this.networkService = ns;
        this.messageService = ms;
        this.requestService = rs;
        this.eventService = es;

        lblWelcome.setText("Salut, @" + user.getUsername() + "!");

        // Folosim Subject fara parametri generici pentru a permite casting-ul
        if (requestService instanceof Subject) {
            ((Subject) requestService).addObserver(this);
        }
        if (messageService instanceof Subject) {
            ((Subject) messageService).addObserver(this);
        }

        if (eventService instanceof Subject) {
            ((Subject<Object>) eventService).addObserver(this);
        }

        checkOfflineEvents();
        refreshBadges();
        goProfile();
    }

    /**
     * Verifica daca exista curse la care utilizatorul a participat
     * si care s-au incheiat in timp ce acesta nu era logat.
     */
    private void checkOfflineEvents() {
        if (eventService == null || currentUser == null) return;

        new Thread(() -> {
            // Folosim getFinishedResultsForUser pentru a primi List<String>
            java.util.List<String> offlineResults = eventService.getFinishedResultsForUser(currentUser.getId());

            for (String result : offlineResults) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Rezultat Cursa (Offline)");
                    alert.setHeaderText("O cursa la care ai participat s-a incheiat!");
                    alert.setContentText(result);
                    alert.show();
                });
            }
        }).start();
    }

    @Override
    public void update(Subject<Object> subject, Object eventData) {
        if (eventData instanceof String resultMessage) {
            // Platform.runLater forteaza aparitia ferestrei pe ecran
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Rezultate Cursa");
                alert.setHeaderText("Cursa s-a finalizat!");
                alert.setContentText(resultMessage);
                alert.show();
            });
        }
        // Update pentru badge-uri
        Platform.runLater(this::refreshBadges);
    }

    // --- LOGICA UI SI NAVIGARE ---

    private void refreshBadges() {
        if (requestService == null || messageService == null) return;
        long notifCount = requestService.getUnseenNotificationCount(currentUser.getId());
        updateBadgeUI(lblNotifBadge, btnNotifications, (int) notifCount, "#c0392b");
        long chatCount = messageService.getUnreadCount(currentUser.getId());
        updateBadgeUI(lblChatBadge, btnChat, (int) chatCount, "#2980b9");
    }

    private void updateBadgeUI(Label badgeLabel, Button btn, int count, String activeColor) {
        if (badgeLabel == null) return;
        if (count > 0) {
            badgeLabel.setVisible(true);
            badgeLabel.setText(String.valueOf(count));
            btn.setStyle("-fx-background-color: " + activeColor + "; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 15; -fx-font-weight: bold;");
        } else {
            badgeLabel.setVisible(false);
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 15; -fx-font-weight: normal;");
        }
    }

    private void setActiveButton(Button active) {
        String base = "-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 15;";
        String activeStyle = "-fx-background-color: #34495e; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 15; -fx-border-color: #3498db; -fx-border-width: 0 0 0 4;";
        btnProfile.setStyle(base); btnChat.setStyle(base); btnSearch.setStyle(base); btnNotifications.setStyle(base);
        if(btnRaces != null) btnRaces.setStyle(base);
        if (active != null) active.setStyle(activeStyle);
    }

    @FXML public void goProfile() { setActiveButton(btnProfile); loadView("/views/ProfileView.fxml", "profile"); }
    @FXML public void goSearch() { setActiveButton(btnSearch); loadView("/views/SearchView.fxml", "search"); }
    @FXML public void goNotifications() { setActiveButton(btnNotifications); loadView("/views/NotificationView.fxml", "notif"); }
    @FXML public void goChat() { setActiveButton(btnChat); loadView("/views/ChatView.fxml", "chat"); }
    @FXML public void goRaces() { setActiveButton(btnRaces); loadView("/views/RaceView.fxml", "races"); }

    private void loadView(String path, String type) {
        try {
            URL resource = getClass().getResource(path);
            if (resource == null) return;
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            switch (type) {
                case "profile" -> ((ProfileController)loader.getController()).setContext(userService, friendshipService, requestService, currentUser, currentUser);
                case "search" -> ((SearchController)loader.getController()).setContext(userService, friendshipService, requestService, currentUser);
                case "notif" -> ((NotificationController)loader.getController()).setContext(requestService, userService, friendshipService, currentUser);
                case "chat" -> ((ChatController)loader.getController()).setContext(messageService, friendshipService, currentUser);
                case "races" -> ((RaceController)loader.getController()).setContext(eventService, currentUser);
            }
            mainLayout.setCenter(view);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleLogout() {
        if (requestService instanceof Subject) ((Subject) requestService).removeObserver(this);
        if (messageService instanceof Subject) ((Subject) messageService).removeObserver(this);
        if (eventService instanceof Subject) ((Subject) eventService).removeObserver(this);
    }
}