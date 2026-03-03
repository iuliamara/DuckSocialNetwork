package com.ubb.gui;

import com.ubb.domain.*;
import com.ubb.utils.paging.Page;
import com.ubb.utils.paging.Pageable;
import com.ubb.service.UserServiceInterface;
import com.ubb.service.FriendshipServiceInterface;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public class UserController {

    // --- FXML ELEMENTS ---
    @FXML private TableView<User> userTableView;
    @FXML private ComboBox<String> userFilterComboBox;
    @FXML private Label userPageLabel;
    @FXML private Button prevUserButton;
    @FXML private Button nextUserButton;

    // Admin Elements
    @FXML private HBox adminActionBox;

    // --- DEPENDENCIES ---
    private UserServiceInterface userService;
    private FriendshipServiceInterface friendshipService;

    // --- STATE ---
    private int userCurrentPage = 1;
    private int userTotalPages = 1;
    private final int pageSize = 10;
    private boolean isAdminMode = false;

    // --- CONFIGURATION ---

    public void setServices(UserServiceInterface userService, FriendshipServiceInterface friendshipService) {
        this.userService = userService;
        this.friendshipService = friendshipService;
        initializeControllerLogic();
    }

    public void setAdminMode(boolean isAdmin) {
        this.isAdminMode = isAdmin;
        if (adminActionBox != null) {
            adminActionBox.setVisible(isAdmin);
            adminActionBox.setManaged(isAdmin);
        }
        loadUserPage(1);
    }

    public void initializeControllerLogic() {
        if(userFilterComboBox != null) {
            userFilterComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                handleFilterChange();
            });
        }
        loadUserPage(1);
    }

    @FXML
    public void initialize() {
        initializeUserTable();

        if (userFilterComboBox != null) {
            userFilterComboBox.getItems().addAll("Toate", "Persoane", "Ducks", "SwimmingDucks", "FlyingDucks", "FlyingAndSwimmingDucks");
            userFilterComboBox.getSelectionModel().selectFirst();
        }

        if (adminActionBox != null) {
            adminActionBox.setVisible(false);
            adminActionBox.setManaged(false);
        }
    }

    // --- TABLE CONFIGURATION ---

    private void initializeUserTable() {
        TableColumn<User, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<User, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<User, String> typeColumn = new TableColumn<>("Tip");
        typeColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getClass().getSimpleName()));

        userTableView.getColumns().setAll(idColumn, usernameColumn, emailColumn, typeColumn);
        userTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void updateTableColumns(String filter) {
        // Reset dynamic columns (keep first 4 fixed columns)
        if (userTableView.getColumns().size() > 4) {
            userTableView.getColumns().remove(4, userTableView.getColumns().size());
        }

        if ("Persoane".equals(filter)) {
            TableColumn<User, String> nume = new TableColumn<>("Nume");
            nume.setCellValueFactory(d -> (d.getValue() instanceof Persoana p) ? new javafx.beans.property.SimpleStringProperty(p.getNume()) : null);

            TableColumn<User, String> prenume = new TableColumn<>("Prenume");
            prenume.setCellValueFactory(d -> (d.getValue() instanceof Persoana p) ? new javafx.beans.property.SimpleStringProperty(p.getPrenume()) : null);

            TableColumn<User, String> job = new TableColumn<>("Ocupatie");
            job.setCellValueFactory(d -> (d.getValue() instanceof Persoana p) ? new javafx.beans.property.SimpleStringProperty(p.getOcupatie()) : null);

            userTableView.getColumns().addAll(nume, prenume, job);

        } else if (filter != null && filter.contains("Duck")) {
            TableColumn<User, Double> v = new TableColumn<>("Viteza");
            v.setCellValueFactory(d -> (d.getValue() instanceof Duck dk) ? new javafx.beans.property.SimpleObjectProperty<>(dk.getViteza()) : null);

            TableColumn<User, Double> r = new TableColumn<>("Rezistenta");
            r.setCellValueFactory(d -> (d.getValue() instanceof Duck dk) ? new javafx.beans.property.SimpleObjectProperty<>(dk.getRezistenta()) : null);

            userTableView.getColumns().addAll(v, r);
        }
    }

    // --- PAGINATION & LOADING ---

    private void loadUserPage(int pageNumber) {
        String currentFilter = userFilterComboBox.getSelectionModel().getSelectedItem();


        try {
            Pageable pageable = new Pageable(pageNumber, pageSize);


            // 1. Actualizeaza setul de coloane in functie de filtru
            updateTableColumns(currentFilter);


            // 2. Apel Service: Obtine datele filtrate si paginate (delegare catre Repository)
            Page<User> userPage = userService.findUsersPaginatedAndFiltered(pageable, currentFilter);
            List<User> userContent = userPage.getContent();


            // 3. Actualizeaza TableView
            userTableView.setItems(FXCollections.observableArrayList(userContent));


            // 4. Actualizeaza starea paginarii
            userCurrentPage = userPage.getPageNumber();
            userTotalPages = userPage.getTotalPages();
            userPageLabel.setText(String.format("Pagina %d din %d (Total %d)",
                    userCurrentPage, userTotalPages, userPage.getTotalElements()));


            // 5. Actualizeaza butoanele Next/Prev (Bindings)
            prevUserButton.setDisable(userPage.getPageNumber() <= 1);
            nextUserButton.setDisable(userPage.getPageNumber() >= userTotalPages);


        } catch (Exception e) {
            System.err.println("Eroare la incarcarea paginii: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Eroare la baza de date", e.getMessage());
        }
    }


    @FXML private void handleNextUserPage() { if (userCurrentPage < userTotalPages) loadUserPage(userCurrentPage + 1); }
    @FXML private void handlePrevUserPage() { if (userCurrentPage > 1) loadUserPage(userCurrentPage - 1); }
    @FXML private void handleFilterChange() { loadUserPage(1); }
    @FXML private void handleRefreshUser() { loadUserPage(userCurrentPage); }

    // --- CRUD OPERATIONS ---

    @FXML
    private void handleAddUser() {
        if (!isAdminMode) return;

        Optional<User> newUserOpt = showUserCreationDialog();
        newUserOpt.ifPresent(newUser -> {
            try {
                // 1. Service adds user
                // 2. Service automatically notifies observers (HomeController, etc.)
                userService.addUser(newUser);

                // 3. Reload local list
                loadUserPage(userCurrentPage);

                showAlert(Alert.AlertType.INFORMATION, "Succes", "Utilizator adaugat!");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Eroare", e.getMessage());
            }
        });
    }

    @FXML private void handleDeleteUser() {
        if (!isAdminMode) return;

        User selected = userTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Atentie", "Selectati un utilizator.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Stergeti userul " + selected.getUsername() + "?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                // 1. Service deletes user
                // 2. Service notifies observers
                userService.deleteUser(selected.getId());

                // 3. Reload local list
                loadUserPage(userCurrentPage);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Eroare", e.getMessage());
            }
        }
    }

    // --- DIALOGS & UTILS ---

    /**
     * Deschide un dialog care colecteaza toate atributele necesare pentru un User (Polimorfism),
     * cu validare vizuala si Binding in timp real.
     * @return Optional<User> - Obiectul User complet construit (cu ID=null)
     */
    private Optional<User> showUserCreationDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Adauga Utilizator Nou");
        dialog.setHeaderText("Introduceti detaliile complete ale utilizatorului (Persoana sau Duck)");


        ButtonType addButton = new ButtonType("Adauga", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);


        // Elemente de Input
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);


        // Campuri Comune
        TextField usernameField = new TextField();
        TextField emailField = new TextField();
        PasswordField passwordField = new PasswordField();


        // Campuri Persoana
        TextField numeField = new TextField();
        TextField prenumeField = new TextField();
        DatePicker dataNasteriiPicker = new DatePicker();
        ComboBox<String> ocupatieField = new ComboBox<>();
        ocupatieField.getItems().addAll("Angajat", "Antreprenor/CEO", "Student/Elev", "Necunoscut/Altele");
        ocupatieField.getSelectionModel().selectFirst();
        TextField empatieField = new TextField("0");


        // Campuri Duck & Tip
        ComboBox<String> typeField = new ComboBox<>();
        typeField.getItems().addAll("Persoana", "SwimmingDuck", "FlyingDuck", "FlyingAndSwimmingDuck");
        typeField.getSelectionModel().selectFirst(); // Seteaza implicit pe Persoana


        TextField vitezaField = new TextField("0.0");
        TextField rezistentaField = new TextField("0.0");


        // Label-uri asociate
        Label numeLabel = new Label("Nume:"); Label prenumeLabel = new Label("Prenume:");
        Label dataNasteriiLabel = new Label("Data Nasterii:"); Label ocupatieLabel = new Label("Ocupatie:");
        Label empatieLabel = new Label("Nivel Empatie:"); Label vitezaLabel = new Label("Viteza:");
        Label rezistentaLabel = new Label("Rezistenta:");


        // Label pentru afisarea erorilor (Binding)
        Label errorLabel = new Label("");
        errorLabel.setTextFill(Color.RED);
        errorLabel.setStyle("-fx-font-weight: bold;");
        errorLabel.setVisible(false);


        // ----------------------------------------------------
        // Asamblarea interfetei (GridPane)
        // ----------------------------------------------------
        grid.addRow(0, new Label("Username:"), usernameField);
        grid.addRow(1, new Label("Email:"), emailField);
        grid.addRow(2, new Label("Password:"), passwordField);
        grid.addRow(3, new Label("--- DETALII TIP ---"));
        grid.addRow(4, new Label("Tip Entitate:"), typeField);
        grid.addRow(5, numeLabel, numeField); grid.addRow(6, prenumeLabel, prenumeField);
        grid.addRow(7, dataNasteriiLabel, dataNasteriiPicker); grid.addRow(8, ocupatieLabel, ocupatieField);
        grid.addRow(9, empatieLabel, empatieField);
        grid.addRow(10, vitezaLabel, vitezaField); grid.addRow(11, rezistentaLabel, rezistentaField);
        grid.addRow(12, errorLabel);


        dialog.getDialogPane().setContent(grid);


        final Button btnAdd = (Button) dialog.getDialogPane().lookupButton(addButton);
        // ----------------------------------------------------




        // ----------------------------------------------------
        // LOGICA DE ASCUNDERE DINAMICA (VISIBILITY)
        // ----------------------------------------------------
        Runnable updateVisibility = () -> {
            String type = typeField.getValue();
            boolean isPersoana = "Persoana".equals(type);
            boolean isDuck = type != null && type.contains("Duck");


            List<Control> persoanaControls = List.of(numeField, prenumeField, dataNasteriiPicker, ocupatieField, empatieField);
            List<Label> persoanaLabels = List.of(numeLabel, prenumeLabel, dataNasteriiLabel, ocupatieLabel, empatieLabel);
            List<Control> duckControls = List.of(vitezaField, rezistentaField);
            List<Label> duckLabels = List.of(vitezaLabel, rezistentaLabel);


            // Folosim stream-uri pentru a seta vizibilitatea (simplitate si concizie)
            persoanaControls.forEach(c -> { c.setVisible(isPersoana); c.setManaged(isPersoana); });
            persoanaLabels.forEach(l -> { l.setVisible(isPersoana); l.setManaged(isPersoana); });
            duckControls.forEach(c -> { c.setVisible(isDuck); c.setManaged(isDuck); });
            duckLabels.forEach(l -> { l.setVisible(isDuck); l.setManaged(isDuck); });


            dialog.getDialogPane().requestLayout();
        };


        // Adauga listener-ul la ComboBox si ruleaza o data
        typeField.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateVisibility.run());
        updateVisibility.run();
        // ----------------------------------------------------




        // ----------------------------------------------------
        // LOGICA DE VALIDARE iN TIMP REAL (BINDINGS)
        // ----------------------------------------------------
        StringBinding validationMessageBinding = Bindings.createStringBinding(() -> {
                    String type = typeField.getValue();


                    // 1. Resetam stilurile de eroare
                    List.of(usernameField, emailField, passwordField, numeField, prenumeField, dataNasteriiPicker, empatieField, vitezaField, rezistentaField).forEach(c -> setInvalidStyle(c, false));


                    // 2. Validare Comuna
                    if (usernameField.getText().trim().isEmpty()) { setInvalidStyle(usernameField, true); return "Username-ul nu poate fi gol."; }
                    if (emailField.getText().trim().isEmpty() || !emailField.getText().contains("@") || !emailField.getText().contains(".")) { setInvalidStyle(emailField, true); return "Format email invalid (lipsesc @ sau .)."; }
                    if (passwordField.getText().trim().isEmpty() || passwordField.getText().length() < 6) { setInvalidStyle(passwordField, true); return "Parola trebuie sa aiba minim 6 caractere."; }


                    // 3. Validare Specifica
                    if ("Persoana".equals(type)) {
                        if (numeField.getText().trim().isEmpty()) { setInvalidStyle(numeField, true); return "Numele este obligatoriu."; }
                        if (prenumeField.getText().trim().isEmpty()) { setInvalidStyle(prenumeField, true); return "Prenumele este obligatoriu."; }
                        if (dataNasteriiPicker.getValue() == null) { setInvalidStyle(dataNasteriiPicker, true); return "Data nasterii este obligatorie."; }
                        if (dataNasteriiPicker.getValue().isAfter(LocalDate.now())) { setInvalidStyle(dataNasteriiPicker, true); return "Data nasterii nu poate fi in viitor."; }
                        if (!isIntegerInRange(empatieField.getText(), 0, 10)) { setInvalidStyle(empatieField, true); return "Nivelul de empatie trebuie sa fie un numar intreg intre 0 si 10."; }
                    } else if (type != null && type.contains("Duck")) {
                        if (!isNumeric(vitezaField.getText()) || Double.parseDouble(vitezaField.getText()) <= 0) { setInvalidStyle(vitezaField, true); return "Viteza trebuie sa fie un numar strict pozitiv (> 0)."; }
                        if (!isNumeric(rezistentaField.getText()) || Double.parseDouble(rezistentaField.getText()) <= 0) { setInvalidStyle(rezistentaField, true); return "Rezistenta trebuie sa fie un numar strict pozitiv (> 0)."; }
                    }


                    // Daca nu s-a gasit nicio eroare
                    return "";


                }, usernameField.textProperty(), emailField.textProperty(), passwordField.textProperty(),
                typeField.valueProperty(), dataNasteriiPicker.valueProperty(),
                numeField.textProperty(), prenumeField.textProperty(), empatieField.textProperty(),
                vitezaField.textProperty(), rezistentaField.textProperty()
        );


        // Leaga butonul si label-ul de Binding-ul de eroare
        btnAdd.disableProperty().bind(validationMessageBinding.isNotEmpty());
        errorLabel.textProperty().bind(validationMessageBinding);
        errorLabel.visibleProperty().bind(validationMessageBinding.isNotEmpty());
        // ----------------------------------------------------




        // Conversia rezultatului (Apelata la apasarea butonului OK)
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButton) {
                try {
                    String type = typeField.getValue();
                    String user = usernameField.getText();
                    String email = emailField.getText();
                    String password = passwordField.getText();


                    return switch (type) {
                        case "Persoana" -> new Persoana(user, email, password, numeField.getText(), prenumeField.getText(), dataNasteriiPicker.getValue(), ocupatieField.getValue(), Integer.parseInt(empatieField.getText()));
                        case "SwimmingDuck" -> new SwimmingDuck(user, email, password, Double.parseDouble(vitezaField.getText()), Double.parseDouble(rezistentaField.getText()));
                        case "FlyingDuck" -> new FlyingDuck(user, email, password, Double.parseDouble(vitezaField.getText()), Double.parseDouble(rezistentaField.getText()));
                        case "FlyingAndSwimmingDuck" -> new FlyingAndSwimmingDuck(user, email, password, Double.parseDouble(vitezaField.getText()), Double.parseDouble(rezistentaField.getText()));
                        default -> null;
                    };
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Eroare Conversie", "Va rugam verificati formatul numeric al campurilor.");
                    return null;
                }
            }
            return null;
        });


        return dialog.showAndWait();
    }


    /**
     * Verifica daca un String este un numar Double valid.
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    /**
     * Aplica sau elimina un stil vizual de eroare (bordura rosie) unui control JavaFX.
     */
    private void setInvalidStyle(Control control, boolean isInvalid) {
        if (isInvalid) {
            control.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
        } else {
            control.setStyle(null); // Elimina stilul de eroare
        }
    }


    /**
     * Verifica daca un String este un numar intreg valid in intervalul [min, max].
     */
    private boolean isIntegerInRange(String str, int min, int max) {
        if (str == null || str.trim().isEmpty()) return false;
        try {
            int value = Integer.parseInt(str);
            return value >= min && value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    /**
     * Metoda auxiliara pentru a afisa un dialog de notificare (Alert).
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}