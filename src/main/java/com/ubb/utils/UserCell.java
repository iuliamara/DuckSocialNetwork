package com.ubb.utils;

import com.ubb.domain.Duck;
import com.ubb.domain.User;
import com.ubb.service.MessageServiceInterface;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UserCell extends ListCell<User> {
    private final MessageServiceInterface messageService;
    private final Long authenticatedUserId;

    // Cache static pentru a evita reîncărcarea imaginilor la fiecare scroll
    private static final Map<String, Image> imageCache = new HashMap<>();

    public UserCell(MessageServiceInterface messageService, Long authenticatedUserId) {
        this.messageService = messageService;
        this.authenticatedUserId = authenticatedUserId;
    }

    @Override
    protected void updateItem(User user, boolean empty) {
        super.updateItem(user, empty);

        if (empty || user == null) {
            setText(null);
            setGraphic(null);
            setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
        } else {
            HBox hBox = new HBox(12);
            hBox.setAlignment(Pos.CENTER_LEFT);

            // --- 1. CONFIGURARE AVATAR ---
            ImageView imageView = new ImageView();
            imageView.setFitHeight(35);
            imageView.setFitWidth(35);
            Circle clip = new Circle(17.5, 17.5, 17.5);
            imageView.setClip(clip);

            // --- 2. LOGICĂ ÎNCĂRCARE IMAGINE ---
            String path = user.getImagePath();
            Image profileImg = null;

            if (path != null && !path.isEmpty()) {
                File file = new File(path);
                if (file.exists()) {
                    profileImg = imageCache.computeIfAbsent(path, p -> new Image("file:" + p, true));
                }
            }

            if (profileImg == null || profileImg.isError()) {
                // Aici am corectat resursele conform logicii tale (duck vs person)
                String resourcePath = (user instanceof Duck) ? "/images/avatar_duck.png" : "/images/avatar_person.png";

                profileImg = imageCache.computeIfAbsent(resourcePath, p -> {
                    try {
                        return new Image(getClass().getResourceAsStream(p));
                    } catch (Exception e) {
                        return null;
                    }
                });
            }
            imageView.setImage(profileImg);

            // --- 3. CONFIGURARE TEXT ȘI NOTIFICĂRI ---
            // Definim variabila o singură dată pentru a evita "already defined in scope"
            Label nameLabel = new Label();

            // Obținem numărul de mesaje necitite de la acest prieten către mine
            long unreadCount = messageService.getUnreadCountFrom(user.getId(), authenticatedUserId);

            if (unreadCount > 0) {
                // Afișează formatul: "Nume (3)"
                nameLabel.setText(user.getUsername() + " (" + unreadCount + ")");

                // Stil de evidențiere (Albastru)
                setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-border-width: 0 0 0 4;");
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1976d2; -fx-font-size: 14px;");
            } else {
                // Afișează doar numele
                nameLabel.setText(user.getUsername());

                // Stil normal
                setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
                nameLabel.setStyle("-fx-font-weight: normal; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
            }

            hBox.getChildren().addAll(imageView, nameLabel);
            setGraphic(hBox);
            setText(null);
        }
    }
}