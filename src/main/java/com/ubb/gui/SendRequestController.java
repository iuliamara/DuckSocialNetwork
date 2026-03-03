package com.ubb.gui;

import com.ubb.domain.*;
import com.ubb.service.FriendshipRequestServiceInterface;
import com.ubb.service.FriendshipServiceInterface;
import com.ubb.service.UserServiceInterface;
import com.ubb.service.exceptions.ServiceException;

import com.ubb.utils.dto.FriendshipDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SendRequestController implements Observer<FriendshipRequest> {

    // --- FXML pentru Tab 1 (Trimitere) ---
    @FXML private ListView<User> userListView;
    @FXML private Button sendRequestButton;
    @FXML private TextField searchField;

    // --- FXML pentru Tab 2 (Cereri Trimise) ---
    @FXML private TableView<FriendshipRequest> sentRequestsTable;
    @FXML private TableColumn<FriendshipRequest, String> sentToColumn;
    @FXML private TableColumn<FriendshipRequest, RequestStatus> sentStatusColumn;
    @FXML private TableColumn<FriendshipRequest, LocalDateTime> sentDateColumn;

    // --- FXML pentru Tab 3 (Prieteni) ---
    @FXML private TableView<FriendshipDTO> friendsTable;
    @FXML private TableColumn<FriendshipDTO, String> friendUsernameColumn;
    @FXML private TableColumn<FriendshipDTO, LocalDateTime> friendSinceColumn;


    // --- MODELE DE DATE ---
    private List<User> allNonFriendsList;
    private final ObservableList<User> userModel = FXCollections.observableArrayList();
    private final ObservableList<FriendshipRequest> sentRequestsModel = FXCollections.observableArrayList();
    private final ObservableList<FriendshipDTO> friendsModel = FXCollections.observableArrayList();


    // --- SERVICE-URI ---
    private UserServiceInterface userService;
    private FriendshipServiceInterface friendshipService;
    private FriendshipRequestServiceInterface requestService;
    private User authenticatedUser;

    public void setServices(UserServiceInterface userService,
                            FriendshipServiceInterface friendshipService,
                            FriendshipRequestServiceInterface requestService,
                            User authenticatedUser) {
        this.userService = userService;
        this.friendshipService = friendshipService;
        this.requestService = requestService;
        this.authenticatedUser = authenticatedUser;

        // 2. ABONARE LA SERVICE
        if (requestService instanceof Subject) {
            ((Subject<FriendshipRequest>) requestService).addObserver(this);
        }

        loadAllData();
    }

    private void loadAllData() {
        loadNonFriends();
        loadSentRequests();
        loadFriends();
    }

    // 3. METODA UPDATE (REACTIA LA SCHIMBARI)
    @Override
    public void update(Subject<FriendshipRequest> subject, FriendshipRequest request) {
        // Verificam daca actualizarea este relevanta pentru utilizatorul curent
        if (authenticatedUser == null) return;

        boolean isRelevant = request.getFromUser().getId().equals(authenticatedUser.getId()) ||
                request.getToUser().getId().equals(authenticatedUser.getId());

        if (isRelevant) {
            // Actualizam UI-ul pe Thread-ul JavaFX
            Platform.runLater(() -> {
                System.out.println("SendRequestController: Detectat schimbare! Reincarc datele...");
                loadAllData();
            });
        }
    }

    // 4. METODA DE DEZABONARE (Pentru a preveni erori la logout)
    public void unsubscribe() {
        if (requestService instanceof Subject) {
            ((Subject<FriendshipRequest>) requestService).removeObserver(this);
        }
    }

    @FXML
    public void initialize() {
        // --- Tab 1 Setup ---
        userListView.setItems(userModel);
        sendRequestButton.setDisable(true);
        userListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        userListView.setCellFactory(listView -> new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUsername());
            }
        });

        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            sendRequestButton.setDisable(newVal == null);
        });

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> filterUsers(newVal));
        }

        // --- Tab 2 Setup (Cereri Trimise) ---
        sentRequestsTable.setItems(sentRequestsModel);
        sentToColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getToUser().getUsername()
        ));
        sentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        sentDateColumn.setCellValueFactory(new PropertyValueFactory<>("dateSent"));

        // FORMATARE: Afiseaza doar data pentru Cererile Trimise
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        sentDateColumn.setCellFactory(column -> new TableCell<FriendshipRequest, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(dateFormatter));
                }
            }
        });

        // --- Tab 3 Setup (Prieteni Actuali) ---
        friendsTable.setItems(friendsModel);

        // Afiseaza Username-ul prietenului
        friendUsernameColumn.setCellValueFactory(data -> {
            FriendshipDTO dto = data.getValue();
            if (authenticatedUser == null) {
                return new javafx.beans.property.SimpleStringProperty("Eroare autentificare");
            }

            String friendUsername = dto.getIdUser1().equals(authenticatedUser.getId()) ? dto.getUsername2() : dto.getUsername1();
            return new javafx.beans.property.SimpleStringProperty(friendUsername);
        });

        //Afiseaza data din DTO
        friendSinceColumn.setCellValueFactory(new PropertyValueFactory<>("friendsFrom"));

        // FORMATARE: Afiseaza doar data pentru Prieteni
        final DateTimeFormatter friendDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        friendSinceColumn.setCellFactory(column -> new TableCell<FriendshipDTO, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(friendDateFormatter));
                }
            }
        });
    }

    // --- LOGICA DE INCARCARE A DATELOR ---

    private void loadNonFriends() {
        if (userService == null || friendshipService == null || authenticatedUser == null) return;
        try {
            // 1. Toti utilizatorii
            List<User> allUsers = StreamSupport.stream(userService.findAllUsers().spliterator(), false)
                    .collect(Collectors.toList());

            // 2. Lista de Prieteni existenti (Status APPROVED)
            List<User> friends = friendshipService.getFriendsOfUser(authenticatedUser.getId());
            List<Long> friendIds = friends.stream().map(User::getId).collect(Collectors.toList());

            // 3. Cereri trimise de mine care sunt PENDING (pe astea le ascundem)
            //Cele REJECTED nu intra aici, deci vor ramane vizibile
            List<FriendshipRequest> sentPending = requestService.getRequestsBySender(authenticatedUser.getId(), RequestStatus.PENDING);
            List<Long> sentPendingIds = sentPending.stream().map(req -> req.getToUser().getId()).collect(Collectors.toList());

            // 4.Cereri primite de mine care sunt PENDING
            // Daca cineva ti-a trimis cerere, nu ii poti trimite si tu inapoi, trebuie sa accepti.
            List<FriendshipRequest> receivedPending = requestService.getRequestsByReceiver(authenticatedUser.getId(), RequestStatus.PENDING);
            List<Long> receivedPendingIds = receivedPending.stream().map(req -> req.getFromUser().getId()).collect(Collectors.toList());

            // 5. Filtrarea Finala
            allNonFriendsList = allUsers.stream()
                    .filter(u -> !u.getId().equals(authenticatedUser.getId())) // Nu eu
                    .filter(u -> !friendIds.contains(u.getId()))               // Nu prieteni
                    .filter(u -> !sentPendingIds.contains(u.getId()))          // Nu cereri trimise in asteptare
                    .filter(u -> !receivedPendingIds.contains(u.getId()))      // Nu cereri primite in asteptare
                    .collect(Collectors.toList());

            userModel.setAll(allNonFriendsList);

            Platform.runLater(() -> userListView.getSelectionModel().clearSelection());

        } catch (ServiceException e) {
            System.err.println("Eroare la incarcarea utilizatorilor non-prieteni: " + e.getMessage());
        }
    }

    private void loadSentRequests() {
        if (requestService == null || authenticatedUser == null) return;
        try {
            List<FriendshipRequest> sentRequests = requestService.getRequestsBySender(authenticatedUser.getId(), null);
            sentRequestsModel.setAll(sentRequests);
        } catch (ServiceException e) {
            System.err.println("Eroare la incarcarea cererilor trimise: " + e.getMessage());
        }
    }

    private void loadFriends() {
        if (friendshipService == null || authenticatedUser == null) return;
        try {
            List<FriendshipDTO> friendDTOs = friendshipService.getFriendshipDTOs(authenticatedUser.getId());
            friendsModel.setAll(friendDTOs);
        } catch (ServiceException e) {
            System.err.println("Eroare la incarcarea prieteniilor: " + e.getMessage());
        }
    }

    // --- GESTIONAREA ACTIUNILOR ---

    @FXML
    private void handleSendRequest() {
        User selectedUser = userListView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) return;

        try {
            requestService.sendRequest(authenticatedUser.getId(), selectedUser.getId());
            loadAllData();
            Platform.runLater(() -> searchField.setText(""));
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Cererea de prietenie a fost trimisa catre " + selectedUser.getUsername() + ".");
        } catch (ServiceException e) {
            showAlert(Alert.AlertType.ERROR, "Eroare Trimitere", e.getMessage());
        }
    }

    private void filterUsers(String searchText) {
        if (allNonFriendsList == null) return;

        if (searchText.trim().isEmpty()) {
            userModel.setAll(allNonFriendsList);
        } else {
            String lowerCaseSearchText = searchText.toLowerCase().trim();
            List<User> filteredList = allNonFriendsList.stream()
                    .filter(user -> user.getUsername().toLowerCase().contains(lowerCaseSearchText))
                    .collect(Collectors.toList());
            userModel.setAll(filteredList);
        }
        Platform.runLater(() -> userListView.getSelectionModel().clearSelection());
    }

    /**
     * Metoda publica de reimprospatare, destinata apelarii din NotificationController
     * (prin intermediul MainController) dupa o actiune de acceptare.
     */
    public void refreshFriendsList() {
        System.out.println("SendRequestController: Reimprospatare lista de prieteni.");
        loadFriends(); // Apel la metoda privata existenta
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}