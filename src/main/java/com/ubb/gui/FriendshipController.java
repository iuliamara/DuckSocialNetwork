package com.ubb.gui;

import com.ubb.domain.Tuple;
import com.ubb.domain.User;
import com.ubb.domain.exceptions.DomainException;
import com.ubb.utils.dto.FriendshipDTO;
import com.ubb.utils.paging.Page;
import com.ubb.utils.paging.Pageable;
import com.ubb.service.FriendshipServiceInterface;
import com.ubb.service.UserServiceInterface;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class FriendshipController {

    // Elemente FXML
    @FXML private TableView<FriendshipDTO> friendshipTableView;
    @FXML private Label friendshipPageLabel;
    @FXML private Button prevFriendshipButton;
    @FXML private Button nextFriendshipButton;

    // Container pentru butoanele de actiune (Add/Delete) - vizibil doar la Admin
    @FXML private HBox adminActionBox;

    // SERVICE DEPENDENCIES
    private FriendshipServiceInterface friendshipService;
    private UserServiceInterface userService;
    private MainController mainController;

    // PAGINATION STATE
    private int friendshipCurrentPage = 1;
    private int friendshipTotalPages = 1;
    private final int pageSize = 10;

    private boolean isAdminMode = false;

    // --- CONFIGURARE ---
    public void setServices(FriendshipServiceInterface friendshipService, UserServiceInterface userService) {
        this.friendshipService = friendshipService;
        this.userService = userService;
        initializeControllerLogic();
    }

    /**
     * Activeaza sau dezactiveaza modul Admin (butoanele de creare/stergere).
     */
    public void setAdminMode(boolean isAdmin) {
        this.isAdminMode = isAdmin;
        if (adminActionBox != null) {
            adminActionBox.setVisible(isAdmin);
            adminActionBox.setManaged(isAdmin);
        }
        refreshPage();
    }

    @FXML
    public void initialize() {
        initializeFriendshipTable();
        // Implicit ascuns pana decide AdminController
        if (adminActionBox != null) {
            adminActionBox.setVisible(false);
            adminActionBox.setManaged(false);
        }
    }

    public void initializeControllerLogic() {
        loadFriendshipPage(1);
    }

    // --- TABEL ---
    private void initializeFriendshipTable() {
        TableColumn<FriendshipDTO, String> user1Column = new TableColumn<>("User 1");
        user1Column.setCellValueFactory(new PropertyValueFactory<>("username1"));

        TableColumn<FriendshipDTO, String> user2Column = new TableColumn<>("User 2");
        user2Column.setCellValueFactory(new PropertyValueFactory<>("username2"));

        TableColumn<FriendshipDTO, LocalDateTime> dateColumn = new TableColumn<>("Din data");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("friendsFrom"));

        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        dateColumn.setCellFactory(column -> new TableCell<FriendshipDTO, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : dateFormatter.format(item));
            }
        });

        friendshipTableView.getColumns().setAll(user1Column, user2Column, dateColumn);
        friendshipTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    // --- PAGINARE ---

    private void loadFriendshipPage(int pageNumber) {
        if (friendshipService == null) return;
        try {
            Pageable pageable = new Pageable(pageNumber, pageSize);
            Page<FriendshipDTO> friendshipPage = friendshipService.findAllPaginated(pageable);

            friendshipTableView.setItems(FXCollections.observableArrayList(friendshipPage.getContent()));

            friendshipCurrentPage = friendshipPage.getPageNumber();
            friendshipTotalPages = friendshipPage.getTotalPages();

            if (friendshipPageLabel != null) {
                int displayTotal = (friendshipTotalPages == 0) ? 1 : friendshipTotalPages;
                friendshipPageLabel.setText(String.format("Pagina %d din %d", friendshipCurrentPage, displayTotal));
            }

            if (prevFriendshipButton != null) prevFriendshipButton.setDisable(friendshipCurrentPage <= 1 );
            if (nextFriendshipButton != null) nextFriendshipButton.setDisable(friendshipCurrentPage >= friendshipTotalPages);

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Eroare DB: " + e.getMessage()).show();
        }
    }

    @FXML private void handleNextFriendshipPage() { if(friendshipCurrentPage<friendshipTotalPages) loadFriendshipPage(friendshipCurrentPage + 1); }
    @FXML private void handlePrevFriendshipPage() { if (friendshipCurrentPage > 1) loadFriendshipPage(friendshipCurrentPage - 1); }

    // --- CRUD (Doar Admin) ---

    @FXML
    private void handleAddFriendship() {
        if (!isAdminMode) return; // Securitate

        Optional<Tuple<Long, Long>> resultOpt = showFriendshipCreationDialog();

        resultOpt.ifPresent(ids -> {
            try {
                // Service-ul va notifica automat Home-ul prin Observer
                friendshipService.addFriendship(ids.getFirst(), ids.getSecond());

                new Alert(Alert.AlertType.INFORMATION, "Prietenie adaugata!").show();
                refreshPage();

            } catch (DomainException e) {
                new Alert(Alert.AlertType.ERROR, "Eroare: " + e.getMessage()).show();
            }
        });
    }

    @FXML
    private void handleDeleteFriendship() {
        if (!isAdminMode) return;

        FriendshipDTO selected = friendshipTableView.getSelectionModel().getSelectedItem();

        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Stergeti prietenia?", ButtonType.YES, ButtonType.NO);
            if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                try {
                    // Service-ul va notifica automat Home-ul prin Observer
                    friendshipService.deleteFriendship(selected.getIdUser1(), selected.getIdUser2());
                    refreshPage();
                } catch (DomainException e) {
                    new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
                }
            }
        } else {
            new Alert(Alert.AlertType.WARNING, "Selectati o prietenie.").show();
        }
    }

    private void refreshPage() {
        loadFriendshipPage(friendshipCurrentPage);
    }

    @FXML private void handleRefreshButtonAction() { refreshPage(); }

    // --- DIALOG COMPLEX---

    private Optional<Tuple<Long, Long>> showFriendshipCreationDialog() {
        Dialog<Tuple<Long, Long>> dialog = new Dialog<>();
        dialog.setTitle("Adauga Prietenie");
        dialog.setHeaderText("Selectati doi utilizatori");

        ButtonType addButton = new ButtonType("Adauga", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);
        final Button btnAdd = (Button) dialog.getDialogPane().lookupButton(addButton);

        List<User> allUsers = StreamSupport.stream(userService.findAllUsers().spliterator(), false)
                .collect(Collectors.toList());

        ComboBox<User> user1ComboBox = createFilterableComboBox(allUsers);
        ComboBox<User> user2ComboBox = createFilterableComboBox(allUsers);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10);
        grid.addRow(0, new Label("User 1:"), user1ComboBox);
        grid.addRow(1, new Label("User 2:"), user2ComboBox);
        dialog.getDialogPane().setContent(grid);

        btnAdd.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        user1ComboBox.getValue() == null || user2ComboBox.getValue() == null ||
                                user1ComboBox.getValue().equals(user2ComboBox.getValue()),
                user1ComboBox.valueProperty(), user2ComboBox.valueProperty()
        ));

        dialog.setResultConverter(btn -> {
            if (btn == addButton) {
                return new Tuple<>(user1ComboBox.getValue().getId(), user2ComboBox.getValue().getId());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private ComboBox<User> createFilterableComboBox(List<User> allUsers) {
        ComboBox<User> comboBox = new ComboBox<>();
        comboBox.setEditable(true);
        comboBox.setVisibleRowCount(5);

        // Initial items
        comboBox.setItems(FXCollections.observableArrayList(allUsers));

        // Converter
        comboBox.setConverter(new javafx.util.StringConverter<User>() {
            @Override public String toString(User u) { return u == null ? "" : u.getUsername(); }
            @Override public User fromString(String s) {
                return allUsers.stream().filter(u -> u.getUsername().equalsIgnoreCase(s)).findFirst().orElse(null);
            }
        });

        // Filter Logic
        comboBox.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (comboBox.getSelectionModel().getSelectedItem() != null &&
                    comboBox.getSelectionModel().getSelectedItem().getUsername().equals(newVal)) {
                return;
            }

            if (newVal == null || newVal.isEmpty()) {
                comboBox.setItems(FXCollections.observableArrayList(allUsers));
                return;
            }

            List<User> filtered = allUsers.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(newVal.toLowerCase()))
                    .collect(Collectors.toList());

            comboBox.setItems(FXCollections.observableArrayList(filtered));

            if (!comboBox.isShowing()) comboBox.show();
        });

        return comboBox;
    }
}