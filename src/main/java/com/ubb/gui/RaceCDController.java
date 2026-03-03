package com.ubb.gui;

import com.ubb.domain.Event;
import com.ubb.domain.RaceEvent;
import com.ubb.service.EventServiceInterface;
import com.ubb.service.exceptions.ServiceException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RaceCDController {

    @FXML private TextField txtRaceName;
    @FXML private TextField txtNumParticipants;
    @FXML private TextField txtBeaconDistances; // Formate: 10, 20, 30
    @FXML private ListView<Event> lstRaces;

    private EventServiceInterface eventService;
    private final ObservableList<Event> raceData = FXCollections.observableArrayList();

    public void setContext(EventServiceInterface eventService) {
        this.eventService = eventService;
        loadRaces();
    }

    private void loadRaces() {
        Iterable<Event> events = eventService.findAllEvents();
        List<Event> list = StreamSupport.stream(events.spliterator(), false)
                .collect(Collectors.toList());
        raceData.setAll(list);
        lstRaces.setItems(raceData);
    }

    @FXML
    public void handleAddRace() {
        try {
            String name = txtRaceName.getText();
            int num = Integer.parseInt(txtNumParticipants.getText());

            // Parsam distantele din String (ex: "10, 50.5, 30") in List<Double>
            List<Double> distances = Arrays.stream(txtBeaconDistances.getText().split(","))
                    .map(String::trim)
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());

            // Apelam metoda de business din service
            eventService.createRaceEvent(name, num, distances);

            clearFields();
            loadRaces();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Cursa a fost creata!");

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Eroare", "Numarul de participanti si distantele trebuie sa fie numerice.");
        } catch (ServiceException e) {
            showAlert(Alert.AlertType.ERROR, "Eroare Service", e.getMessage());
        }
    }

    @FXML
    public void handleDeleteRace() {
        Event selected = lstRaces.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Atentie", "Selectati o cursa pentru a o sterge.");
            return;
        }
        try {
            eventService.deleteEvent(selected.getId());

            loadRaces();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Cursa a fost stearsa.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-a putut sterge cursa.");
        }
    }

    private void clearFields() {
        txtRaceName.clear();
        txtNumParticipants.clear();
        txtBeaconDistances.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}