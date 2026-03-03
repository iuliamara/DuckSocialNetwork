package com.ubb.gui;

import com.ubb.domain.Friendship;
import com.ubb.domain.Subject;
import com.ubb.domain.User;
import com.ubb.domain.Observer;
import com.ubb.service.FriendshipServiceInterface;
import com.ubb.service.NetworkServiceInterface;
import com.ubb.service.UserServiceInterface;
import com.ubb.utils.dto.NetworkStats;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Controller pentru Dashboard-ul de statistici integrat în AdminPanel.
 * Implementeaza Observer pentru a reactiona la schimbarile din retea.
 */
public class HomeController {

    // --- ELEMENTE FXML ---
    @FXML private Label titleLabel;
    @FXML private ImageView duckImageView;
    @FXML private Label communitiesCountLabel;
    @FXML private Label longestPathLengthLabel;
    @FXML private Label longestPathUsersLabel;
    @FXML private Label statusLabel;
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalFriendships;

    // --- DEPENDENTE ---
    private NetworkServiceInterface networkService;
    private UserServiceInterface userService;
    private FriendshipServiceInterface friendshipService;

    // --- OBSERVERS (Pentru update automat) ---
    private Observer<User> userObserver;
    private Observer<Friendship> friendshipObserver;

    /**
     * Seteaza contextul serviciilor si porneste monitorizarea retelei.
     */
    public void setContext(NetworkServiceInterface networkService,
                           UserServiceInterface userService,
                           FriendshipServiceInterface friendshipService) {
        this.networkService = networkService;
        this.userService = userService;
        this.friendshipService = friendshipService;

        initializeView();
        setupObservers();
        loadNetworkStatistics(); // Prima incarcare la deschidere
    }

    @FXML
    public void initialize() {
        // Initializari UI de baza
        if (statusLabel != null) statusLabel.setText("Pregatit.");
    }

    private void initializeView() {
        if (titleLabel != null) titleLabel.setText("Duck Social Network - Statistici");
        if (duckImageView != null) {
            try {
                // Incarcam imaginea de profil a aplicatiei
                Image duckImage = new Image(getClass().getResourceAsStream("/images/duckHome.png"));
                duckImageView.setImage(duckImage);
            } catch (Exception e) {
                System.err.println("Imaginea implicita nu a putut fi gasita in resurse.");
            }
        }
    }

    // --- LOGICA DE OBSERVER (Sincronizare in timp real) ---

    private void setupObservers() {
        // Observer pentru schimbari in lista de utilizatori
        this.userObserver = (subject, user) -> {
            System.out.println("Update detectat: Utilizatori. Reincarcare statistici...");
            loadNetworkStatistics();
        };

        // Observer pentru schimbari in lista de prietenii
        this.friendshipObserver = (subject, friendship) -> {
            System.out.println("Update detectat: Prietenii. Reincarcare statistici...");
            loadNetworkStatistics();
        };

        // Abonarea la servicii daca acestea permit acest lucru (implementeaza Subject)
        if (userService instanceof Subject) {
            ((Subject<User>) userService).addObserver(userObserver);
        }
        if (friendshipService instanceof Subject) {
            ((Subject<Friendship>) friendshipService).addObserver(friendshipObserver);
        }
    }

    /**
     * Dezabonare la inchiderea ferestrei pentru a evita memory leaks.
     */
    public void unsubscribe() {
        if (userService instanceof Subject && userObserver != null) {
            ((Subject<User>) userService).removeObserver(userObserver);
        }
        if (friendshipService instanceof Subject && friendshipObserver != null) {
            ((Subject<Friendship>) friendshipService).removeObserver(friendshipObserver);
        }
    }

    // --- CALCUL ASINCRON (Pentru a nu bloca Panoul Admin) ---

    public void loadNetworkStatistics() {
        if (networkService == null) return;

        Platform.runLater(() -> {
            if (statusLabel != null) statusLabel.setText("Calculare algoritmi graf...");
        });

        // Folosim Task pentru a rula algoritmii de graf pe un thread separat
        Task<NetworkStats> task = new Task<>() {
            @Override
            protected NetworkStats call() throws Exception {
                // Determinam componentele conexe și cel mai lung drum
                int communities = networkService.getNumberOfCommunities();
                List<User> path = networkService.getMostSociableCommunity();

                // Numaram entitatile folosind stream-uri
                long totalU = StreamSupport.stream(userService.findAllUsers().spliterator(), false).count();
                long totalF = StreamSupport.stream(friendshipService.findAllFriendships().spliterator(), false).count();

                return new NetworkStats(communities, path, totalU, totalF);
            }
        };

        task.setOnSucceeded(event -> {
            NetworkStats stats = task.getValue();

            // Actualizam etichetele pe thread-ul principal de UI
            if (communitiesCountLabel != null)
                communitiesCountLabel.setText("Numar Comunitati: " + stats.communityCount());

            if (lblTotalUsers != null)
                lblTotalUsers.setText("Total Utilizatori: " + stats.totalUsers());

            if (lblTotalFriendships != null)
                lblTotalFriendships.setText("Total Prietenii: " + stats.totalFriendships());

            int pathLen = stats.longestPathUsers().size();
            if (longestPathLengthLabel != null)
                longestPathLengthLabel.setText("Cea mai sociabila comunitate: " + pathLen + " membri");

            if (longestPathUsersLabel != null) {
                if (pathLen > 0) {
                    String names = stats.longestPathUsers().stream()
                            .map(User::getUsername)
                            .limit(8)
                            .collect(Collectors.joining(", "));
                    if (pathLen > 8) names += "...";
                    longestPathUsersLabel.setText("Membri cheie: " + names);
                } else {
                    longestPathUsersLabel.setText("Membri: -");
                }
            }

            if (statusLabel != null) statusLabel.setText("Statistici actualizate la zi.");
        });

        task.setOnFailed(e -> {
            if (statusLabel != null) statusLabel.setText("Eroare la procesarea grafului.");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}