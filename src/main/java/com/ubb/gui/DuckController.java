package com.ubb.gui;

import com.ubb.domain.Duck;
import com.ubb.domain.SwimmingDuck;
import com.ubb.domain.FlyingDuck;
import com.ubb.domain.FlyingAndSwimmingDuck;
import com.ubb.service.UserServiceInterface;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DuckController {

    // --- Serviciu Injectat din MainFX ---
    private UserServiceInterface userService;

    // --- Elemente UI Injectate din FXML (fx:id) ---
    @FXML
    private ComboBox<String> filterComboBox;

    // CRITIC FIX: TableView trebuie sa fie de tip Duck pentru a accesa viteza/rezistenta
    @FXML
    private TableView<Duck> duckTableView;

    // CRITIC FIX: Modelul observabil stocheaza obiecte Duck
    private ObservableList<Duck> duckModel = FXCollections.observableArrayList();

    /**
     * Setarea Serviciului (Dependency Injection)
     * Apelat de MainFX.java dupa incarcarea FXML-ului.
     */
    public void setUserService(UserServiceInterface userService) {
        this.userService = userService;
        initializeTableView();
        loadData("Toate");
    }

    /**
     * Metoda apelata automat de FXMLLoader dupa ce toate elementele @FXML sunt injectate.
     */
    @FXML
    public void initialize() {
        // Initializarea coloanelor ComboBox-ului (ex: filtre dupa tip)
        filterComboBox.getItems().addAll("Toate", "SwimmingDuck", "FlyingDuck", "FlyingAndSwimmingDuck");
        filterComboBox.getSelectionModel().selectFirst();
    }

    /**
     * Initializeaza coloanele TableView-ului.
     */
    private void initializeTableView() {
        // Coloana ID
        TableColumn<Duck, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        // Coloana Username
        TableColumn<Duck, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        // Coloana Viteza (Proprietate specifica Duck)
        TableColumn<Duck, Double> vitezaColumn = new TableColumn<>("Viteza");
        vitezaColumn.setCellValueFactory(new PropertyValueFactory<>("viteza"));

        // Coloana Rezistenta (Proprietate specifica Duck)
        TableColumn<Duck, Double> rezistentaColumn = new TableColumn<>("Rezistenta");
        rezistentaColumn.setCellValueFactory(new PropertyValueFactory<>("rezistenta"));

        // Coloana Tip (Informativ, bazat pe instanceof)
        TableColumn<Duck, String> typeColumn = new TableColumn<>("Tip");
        typeColumn.setCellValueFactory(data -> {
            Duck duck = data.getValue();
            // Logica determina ce clasă concretă este (SwimmingDuck, FlyingDuck, etc.)
            if (duck instanceof FlyingAndSwimmingDuck) {
                return new javafx.beans.property.SimpleStringProperty(DuckCapability.DUAL.toString());
            } else if (duck instanceof SwimmingDuck) {
                return new javafx.beans.property.SimpleStringProperty(DuckCapability.SWIMMING.toString());
            } else if (duck instanceof FlyingDuck) {
                return new javafx.beans.property.SimpleStringProperty(DuckCapability.FLYING.toString());
            } else {
                return new javafx.beans.property.SimpleStringProperty("Necunoscut");
            }
        });

        duckTableView.getColumns().clear();
        duckTableView.getColumns().addAll(idColumn, usernameColumn, vitezaColumn, rezistentaColumn, typeColumn);
        duckTableView.setItems(duckModel);
    }


    /**
     * incarca toate obiectele Duck din Service si le plaseaza in modelul TableView.
     */
    private void loadData(String userType) {

        // 1. Controller-ul deleaga cererea de filtrare catre Service
        List<Duck> filteredList = userService.findDucksByType(userType);

        // 2. Seteaza datele
        ObservableList<Duck> observableList = FXCollections.observableArrayList(filteredList);
        duckTableView.setItems(observableList);
    }

    // --- Handlere de Actiuni (onAction din FXML) ---

    /**
     * Apelat cand se apasa butonul "Reincarca Date" (sau la init).
     */
    @FXML
    private void handleRefresh() {
        String currentFilter = filterComboBox.getSelectionModel().getSelectedItem();
        loadData(currentFilter); // Reincarca toate datele din BD
        System.out.println("Date reincarcate.");
    }

    /**
     * Apelat cand se schimba selectia in ComboBox.
     */
    @FXML
    private void handleFilterChange() {
        String selectedFilter = filterComboBox.getSelectionModel().getSelectedItem();

        // Controller-ul paseaza responsabilitatea filtrarii catre Service
        loadData(selectedFilter);
    }
}