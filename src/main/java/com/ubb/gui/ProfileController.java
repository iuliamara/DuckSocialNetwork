package com.ubb.gui;

import com.ubb.domain.*;
import com.ubb.service.FriendshipRequestServiceInterface;
import com.ubb.service.FriendshipServiceInterface;
import com.ubb.service.UserServiceInterface;
import com.ubb.service.exceptions.ServiceException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.List;

public class ProfileController implements Observer<User> {

    @FXML private ImageView profileImageView;
    @FXML private Label lblUsername, lblFullName, lblEmail, lblType;
    @FXML private VBox customAttributesContainer;
    @FXML private HBox requestActionsContainer;
    @FXML private Button btnChangePicture, btnAddFriend;
    @FXML private TextField txtSearchFriends;
    @FXML private ListView<User> friendsListView;

    private UserServiceInterface userService;
    private FriendshipServiceInterface friendshipService;
    private FriendshipRequestServiceInterface requestService;
    private User authenticatedUser;
    private User displayedUser;

    private final ObservableList<User> friendsMasterList = FXCollections.observableArrayList();

    public void setContext(UserServiceInterface us, FriendshipServiceInterface fs,
                           FriendshipRequestServiceInterface rs, User authUser, User targetUser) {
        this.userService = us;
        this.friendshipService = fs;
        this.requestService = rs;
        this.authenticatedUser = authUser;
        this.displayedUser = targetUser;

        boolean isSelf = authUser.getId().equals(targetUser.getId());

        // Vizibilitate de baza
        btnChangePicture.setVisible(isSelf);
        btnChangePicture.setManaged(isSelf);

        updateActionButtons(isSelf);

        userService.addObserver(this);
        initUserProfile();
        initFriendsListLogic();
    }

    private void updateActionButtons(boolean isSelf) {
        if (isSelf) {
            btnAddFriend.setVisible(false);
            btnAddFriend.setManaged(false);
            requestActionsContainer.setVisible(false);
            requestActionsContainer.setManaged(false);
            return;
        }

        boolean areFriends = friendshipService.areFriends(authenticatedUser.getId(), displayedUser.getId());

        // Verificam daca am PRIMIT o cerere de la acest utilizator
        boolean hasReceivedRequest = requestService.getNotifications(authenticatedUser.getId()).stream()
                .anyMatch(r -> r.getFromUser().getId().equals(displayedUser.getId()) &&
                        r.getStatus() == RequestStatus.PENDING);

        // Verificam daca am TRIMIS deja o cerere catre acest utilizator
        boolean hasSentRequest = requestService.getRequestsBySender(authenticatedUser.getId(), RequestStatus.PENDING).stream()
                .anyMatch(r -> r.getToUser().getId().equals(displayedUser.getId()));

        if (areFriends) {
            btnAddFriend.setVisible(true);
            btnAddFriend.setManaged(true);
            btnAddFriend.setText("Sunteti prieteni");
            btnAddFriend.setDisable(true);
            btnAddFriend.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 20;");
            requestActionsContainer.setVisible(false);
            requestActionsContainer.setManaged(false);
        } else if (hasReceivedRequest) {
            // Daca am primit cerere, aratam butoanele de Accept/Reject
            btnAddFriend.setVisible(false);
            btnAddFriend.setManaged(false);
            requestActionsContainer.setVisible(true);
            requestActionsContainer.setManaged(true);
        } else if (hasSentRequest) {
            // Daca am trimis deja cerere, dezactivam butonul si schimbam textul
            btnAddFriend.setVisible(true);
            btnAddFriend.setManaged(true);
            btnAddFriend.setText("Cerere in asteptare");
            btnAddFriend.setDisable(true);
            btnAddFriend.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-background-radius: 20;");
            requestActionsContainer.setVisible(false);
            requestActionsContainer.setManaged(false);
        } else {
            // Cazul in care nu exista nicio relatie sau cerere
            btnAddFriend.setVisible(true);
            btnAddFriend.setManaged(true);
            btnAddFriend.setText("➕ Adauga Prieten");
            btnAddFriend.setDisable(false);
            btnAddFriend.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 20;");
            requestActionsContainer.setVisible(false);
            requestActionsContainer.setManaged(false);
        }
    }

    private void initUserProfile() {
        // Username mare cu @
        lblFullName.setText("@" + displayedUser.getUsername());
        lblEmail.setText(displayedUser.getEmail());
        lblType.setText(displayedUser.getClass().getSimpleName());

        loadProfileImage(displayedUser, profileImageView, 90);
        displayCustomAttributes();
    }

    private void displayCustomAttributes() {
        customAttributesContainer.getChildren().clear();
        Label title = new Label("DETALII SPECIFICE");
        title.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #95a5a6;");
        customAttributesContainer.getChildren().add(title);

        if (displayedUser instanceof Persoana p) {
            addAttributeLabel("Prenume: " + p.getPrenume());
            addAttributeLabel("Nume: " + p.getNume());
            addAttributeLabel("Ocupatie: " + p.getOcupatie());
            addAttributeLabel("Empatie: " + p.getNivelEmpatie());
        } else if (displayedUser instanceof Duck d) {
            addAttributeLabel("Viteza: " + d.getViteza() + " m/s");
            addAttributeLabel("Rezistenta: " + d.getRezistenta());
        }
    }

    private void addAttributeLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        customAttributesContainer.getChildren().add(label);
    }

    private void initFriendsListLogic() {
        friendsListView.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User friend, boolean empty) {
                super.updateItem(friend, empty);
                if (empty || friend == null) { setGraphic(null); setText(null); }
                else {
                    HBox cell = new HBox(12); cell.setAlignment(Pos.CENTER_LEFT);
                    ImageView thumb = new ImageView(); thumb.setFitWidth(35); thumb.setFitHeight(35);
                    loadProfileImage(friend, thumb, 17.5);
                    Label name = new Label(friend.getUsername()); cell.getChildren().addAll(thumb, name);
                    setGraphic(cell);
                }
            }
        });

        FilteredList<User> filteredFriends = new FilteredList<>(friendsMasterList, p -> true);
        txtSearchFriends.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredFriends.setPredicate(friend -> {
                if (newVal == null || newVal.isEmpty()) return true;
                return friend.getUsername().toLowerCase().contains(newVal.toLowerCase());
            });
        });
        friendsListView.setItems(filteredFriends);
        friendsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                User selected = friendsListView.getSelectionModel().getSelectedItem();
                if (selected != null) setContext(userService, friendshipService, requestService, authenticatedUser, selected);
            }
        });
        loadFriendsData();
    }

    private void loadFriendsData() {
        List<User> friends = friendshipService.getAllFriends(displayedUser.getId());
        friendsMasterList.setAll(friends);
    }

    private void loadProfileImage(User user, ImageView imageView, double radius) {
        String path = user.getImagePath();
        Image img;
        if (path != null && !path.isEmpty()) img = new Image("file:" + path);
        else {
            String res = (user instanceof Persoana) ? "/images/avatar_person.png" : "/images/avatar_duck.png";
            img = new Image(getClass().getResourceAsStream(res));
        }
        imageView.setImage(img);
        imageView.setClip(new Circle(radius, radius, radius));
    }

    @FXML
    public void onAcceptRequest() {
        try {
            FriendshipRequest req = requestService.getNotifications(authenticatedUser.getId()).stream()
                    .filter(r -> r.getFromUser().getId().equals(displayedUser.getId()) &&
                            r.getStatus() == RequestStatus.PENDING)
                    .findFirst()
                    .orElseThrow(() -> new ServiceException("Cererea nu a fost gasita."));

            requestService.acceptRequest(req.getId()); // Aceasta metoda creeaza si prietenia

            updateActionButtons(false);
            loadFriendsData();
            new Alert(Alert.AlertType.INFORMATION, "Cerere acceptata!").show();
        } catch (ServiceException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    public void onRejectRequest() {
        try {
            FriendshipRequest req = requestService.getNotifications(authenticatedUser.getId()).stream()
                    .filter(r -> r.getFromUser().getId().equals(displayedUser.getId()) &&
                            r.getStatus() == RequestStatus.PENDING)
                    .findFirst()
                    .orElseThrow(() -> new ServiceException("Cererea nu a fost gasita."));

            requestService.rejectRequest(req.getId());

            updateActionButtons(false);
            new Alert(Alert.AlertType.INFORMATION, "Cerere respinsa.").show();
        } catch (ServiceException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    public void onAddFriendAction() {
        try {
            // Trimitem cererea prin serviciu
            requestService.sendRequest(authenticatedUser.getId(), displayedUser.getId());

            // Apelam aceasta metoda pentru a re-evalua starea relatiei
            // si a schimba butonul in "Cerere Trimisa" sau a-l ascunde
            updateActionButtons(false);

            new Alert(Alert.AlertType.INFORMATION, "Cerere trimisa cu succes!").show();
        } catch (ServiceException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    @FXML
    public void onChangePicture() { //ar fi de preferat in baza de date sa salvezi direct fotografia
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                userService.updateOnlyPhoto(displayedUser.getId(), file.getAbsolutePath());
                displayedUser.setImagePath(file.getAbsolutePath());
                loadProfileImage(displayedUser, profileImageView, 90);
            } catch (Exception e) { new Alert(Alert.AlertType.ERROR, e.getMessage()).show(); }
        }
    }

    @Override
    public void update(Subject<User> subject, User updatedUser) {
        Platform.runLater(() -> {
            if (updatedUser.getId().equals(displayedUser.getId())) {
                this.displayedUser = updatedUser;
                initUserProfile();
            }
        });
    }
}