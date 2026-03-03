package com.ubb.gui;

import com.ubb.domain.*;
import com.ubb.service.EventServiceInterface;
import com.ubb.service.exceptions.ServiceException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RaceController {

    @FXML private ListView<Event> raceListView;
    @FXML private Button btnSubscribe;
    @FXML private Button btnStartRace;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblStatus;

    private EventServiceInterface eventService;
    private User authenticatedUser;
    private final ObservableList<Event> raceData = FXCollections.observableArrayList();

    public void setContext(EventServiceInterface eventService, User authenticatedUser) {
        this.eventService = eventService;
        this.authenticatedUser = authenticatedUser;

        initRaceList();
        loadEvents();
    }

    /**
     * Initializeaza lista si logica de afisare a statusului de abonat.
     */
    private void initRaceList() {
        raceListView.setCellFactory(lv -> new ListCell<Event>() {
            @Override
            protected void updateItem(Event item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String status = item.isFinished() ? "[TERMINAT]" : "[ACTIV]";

                    // Verificam daca utilizatorul curent este deja in lista de observatori
                    boolean isSubscribed = item.getSubscribers().stream()
                            .anyMatch(obs -> obs instanceof User && ((User) obs).getId().equals(authenticatedUser.getId()));

                    String subscriptionText = isSubscribed ? " (Abonat)" : "";
                    setText(status + " " + item.getName() + subscriptionText);

                    // Feedback vizual prin culori
                    if (isSubscribed) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: black; -fx-font-weight: normal;");
                    }
                }
            }
        });

        raceListView.setItems(raceData);

        // Dezactivam butoanele in functie de selectie si status
        raceListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateButtonStates(newVal);
        });
    }

    private void updateButtonStates(Event selected) {
        if (selected == null) {
            btnSubscribe.setDisable(true);
            btnSubscribe.setText("Participa/Aboneaza");
            btnStartRace.setDisable(true);
            return;
        }

        // Verificam daca utilizatorul curent se afla in lista de subscriptii
        boolean isSubscribed = selected.getSubscribers().stream()
                .anyMatch(o -> o instanceof User && ((User) o).getId().equals(authenticatedUser.getId()));

        if (isSubscribed) {
            btnSubscribe.setDisable(true); // Prevenim abonarea multipla
            btnSubscribe.setText("Deja Abonat"); // Feedback vizual clar pentru utilizator
        } else if (selected.isFinished()) {
            btnSubscribe.setDisable(true);
            btnSubscribe.setText("Cursa Terminata");
        } else {
            btnSubscribe.setDisable(false);
            btnSubscribe.setText("🔔 Participa/Aboneaza");
        }

        btnStartRace.setDisable(selected.isFinished());
    }

    private void loadEvents() {
        // Preluam toate evenimentele prin serviciu
        Iterable<Event> events = eventService.findAllEvents();
        List<Event> list = StreamSupport.stream(events.spliterator(), false)
                .collect(Collectors.toList());
        raceData.setAll(list);
    }

    /**
     * Actiunea de abonare (M:N) la eveniment.
     */
    @FXML
    public void onSubscribe() {
        Event selected = raceListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            eventService.subscribe(selected.getId(), authenticatedUser.getId());

            // Reincarcam evenimentele (care acum vor fi reconstruite cu noul abonat)
            loadEvents();

            // Re-evaluam starea butoanelor pentru elementul care a ramas selectat
            updateButtonStates(raceListView.getSelectionModel().getSelectedItem());

            showAlert("Succes", "Te-ai înscris la: " + selected.getName());
        } catch (ServiceException e) {
            showAlert("Eroare", e.getMessage());
        }
    }

    /**
     * Declanseaza cursa in mod ASINCRON.
     */
    @FXML
    public void onStartRace() {
        Event selected = raceListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isFinished()) return;

        setLoadingState(true);
        lblStatus.setText("Cursa se desfasoara asincron...");

        // Apel asincron catre serviciu folosind CompletableFuture
        eventService.finishEventAsync(selected.getId())
                .thenAccept(resultMessage -> {
                    // Revenim pe thread-ul de UI pentru actualizarea finala
                    Platform.runLater(() -> {
                        setLoadingState(false);
                        lblStatus.setText("Cursa finalizata.");
                        showResultDialog(selected.getName(), resultMessage);
                        loadEvents();
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoadingState(false);
                        lblStatus.setText("Eroare la procesare.");
                        showAlert("Eroare", ex.getMessage());
                    });
                    return null;
                });
    }

    private void setLoadingState(boolean loading) {
        progressIndicator.setVisible(loading);
        btnStartRace.setDisable(loading);
        btnSubscribe.setDisable(loading);
    }

    private void showResultDialog(String raceName, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rezultate: " + raceName);
        alert.setHeaderText("Clasamentul Final (Notificat tuturor abonatilor)");

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}