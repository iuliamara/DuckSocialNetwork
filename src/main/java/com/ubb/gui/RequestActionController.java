package com.ubb.gui;

import com.ubb.domain.FriendshipRequest;
import com.ubb.service.FriendshipRequestServiceInterface;
import com.ubb.service.exceptions.ServiceException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Controller pentru fereastra modala de Accept/Respinge o cerere de prietenie.
 */
public class RequestActionController {

    @FXML private Label requestDetailsLabel;
    @FXML private Button acceptButton;
    @FXML private Button declineButton;

    private FriendshipRequestServiceInterface requestService;
    private FriendshipRequest currentRequest;
    private Runnable refreshCallback;

    public void setRefreshCallback(Runnable callback) {
        this.refreshCallback = callback;
    }

    /**
     * Seteaza dependentele (Service) si cererea care urmeaza a fi procesata.
     * Aceasta este metoda de injectie apelata din FriendRequestController.
     */
    public void setServices(FriendshipRequestServiceInterface requestService, FriendshipRequest request) {
        this.requestService = requestService;
        this.currentRequest = request;

        if (request != null) {
            String fromUserUsername = request.getFromUser().getUsername();
            requestDetailsLabel.setText("Doriti sa acceptati cererea de prietenie de la " + fromUserUsername + "?");
        } else {
            requestDetailsLabel.setText("Eroare: Nu s-a putut incarca cererea.");
            acceptButton.setDisable(true);
            declineButton.setDisable(true);
        }
    }

    /**
     * Gestioneaza actiunea de Acceptare a cererii.
     */
    @FXML
    private void handleAccept() {
        if (currentRequest == null) return;

        try {
            // Logica Service: accepta cererea
            requestService.acceptRequest(currentRequest.getId());
            // Apelam callback-ul pentru a actualiza lista de prieteni in tab-ul corespunzator
            if (refreshCallback != null) {
                Platform.runLater(refreshCallback); // Asigura ca reimprospatarea UI se face pe firul FX
            }

            // inchide fereastra imediat dupa succes, pentru a permite Observer-ului sa ruleze rapid.
            closeWindow();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Prietenie acceptata. S-a creat o noua relatie!");

        } catch (ServiceException e) {
            showAlert(Alert.AlertType.ERROR, "Eroare Acceptare", "Nu s-a putut accepta cererea: " + e.getMessage());
        }
    }

    /**
     * Gestioneaza actiunea de Respingere a cererii.
     */
    @FXML
    private void handleDecline() {
        if (currentRequest == null) return;

        try {
            // Logica Service: respinge cererea
            requestService.rejectRequest(currentRequest.getId());

            // Acesta va reincarca toate listele din SendRequestController (prieteni, cereri trimise, utilizatori)
            if (refreshCallback != null) {
                Platform.runLater(refreshCallback);
            }

            // inchide fereastra imediat dupa succes
            closeWindow();

            Platform.runLater(() -> {
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Cerere respinsa.");
            });
        } catch (ServiceException e) {
            showAlert(Alert.AlertType.ERROR, "Eroare Respingere", "Nu s-a putut respinge cererea: " + e.getMessage());
        }
    }

    /**
     * Metoda utilitara pentru a inchide fereastra modala.
     */
    private void closeWindow() {
        // Obtine Stage-ul curent (fereastra) si il inchide
        Stage stage = (Stage) acceptButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Metoda utilitara pentru afisarea alertelor.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show(); // Afiseaza fara a bloca Thread-ul.
    }
}