package com.ubb.gui;

import com.ubb.service.EventServiceInterface;
import com.ubb.service.FriendshipServiceInterface;
import com.ubb.service.NetworkServiceInterface;
import com.ubb.service.UserServiceInterface;
import javafx.fxml.FXML;

public class AdminController {

    // --- 1. INJECTARE CONTROLLERE COPIL (DIN FXML) ---
    // Numele variabilei trebuie să fie [fx:id din FXML] + "Controller"

    @FXML private UserController tabUseriController;

    @FXML private FriendshipController tabPrieteniiController;

    @FXML HomeController tabHomeController;

    @FXML private RaceCDController tabRaceController;

    // --- 2. SERVICIILE ---
    private UserServiceInterface userService;
    private FriendshipServiceInterface friendshipService;
    private NetworkServiceInterface networkService;
    private EventServiceInterface eventService;

    /**
     * Metoda apelata din LauncherController.
     */
    public void setServices(UserServiceInterface userService, FriendshipServiceInterface friendshipService, NetworkServiceInterface networkService,EventServiceInterface eventService) {
        this.userService = userService;
        this.friendshipService = friendshipService;
        this.networkService = networkService;
        this.eventService = eventService;

        initChildControllers();
    }

    private void initChildControllers() {
        // A. Configuram UserController
        if (tabUseriController != null) {
            // 1. Injectam serviciile (ca sa se populeze tabelul)
            tabUseriController.setServices(userService, friendshipService);

            // 2. Activam butoanele de Admin (Adauga/Sterge)
            tabUseriController.setAdminMode(true);

            System.out.println("AdminController: UserController configurat cu succes.");
        } else {
            System.err.println("EROARE: tabUseriController este NULL!");
        }

        // B. Configuram FriendshipController
        if (tabPrieteniiController != null) {
            // 1. Injectam serviciile
            tabPrieteniiController.setServices(friendshipService, userService);

            // 2. Activam butoanele de Admin
            tabPrieteniiController.setAdminMode(true);

            System.out.println("AdminController: FriendshipController configurat cu succes.");
        } else {
            System.err.println("EROARE: tabPrieteniiController este NULL!");
        }

        //C. Configuram HomeController
        if(tabHomeController != null) {
            // 1. Injectam serviciile
            tabHomeController.setContext(networkService,userService,friendshipService);
            System.out.println("AdminController: HomeController configurat cu succes.");
        } else {
            System.out.println("EROARE: tabHomeController este NULL!");
        }

        if (tabRaceController != null) {
            // Injectam serviciul de evenimente necesar pentru Add/View
            tabRaceController.setContext(eventService);
            System.out.println("AdminController: RaceCDController configurat cu succes.");
        } else {
            System.err.println("EROARE: tabRaceController este NULL!");
        }
    }
}