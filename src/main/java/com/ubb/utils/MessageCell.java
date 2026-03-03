package com.ubb.utils;

import com.ubb.domain.Message;
import com.ubb.domain.User;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MessageCell extends ListCell<Message> {
    private final User authenticatedUser;

    public MessageCell(User authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    @Override
    protected void updateItem(Message msg, boolean empty) {
        super.updateItem(msg, empty);

        if (empty || msg == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        boolean isSentByMe = msg.getFrom() != null && msg.getFrom().getId().equals(authenticatedUser.getId());

        // --- 1. Definirea Stilurilor ---
        final String bubbleColor = isSentByMe ? "#007bff" : "#f0f0f0";
        final String textColor = isSentByMe ? "#ffffff" : "#333333";
        final Pos alignment = isSentByMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT;
        final String replyTextColor = isSentByMe ? "#d1d1d1" : "#888888";
        final Pos internalAlignment = isSentByMe ? Pos.TOP_RIGHT : Pos.TOP_LEFT; // Aliniere internă

        // --- 2. Containerul Bulei (VBox) ---
        VBox bubbleContent = new VBox(2);
        bubbleContent.setMaxWidth(350);
        bubbleContent.setAlignment(internalAlignment); // FORȚEAZĂ elementele din VBox să stea la dreapta/stânga

        // --- 3. LOGICĂ REPLY ---
        if (msg.getReply() != null) {
            Message originalMsg = msg.getReply();
            String senderName = (originalMsg.getFrom() != null) ? originalMsg.getFrom().getUsername() : "User";
            String fullText = (originalMsg.getMessage() != null) ? originalMsg.getMessage() : "";
            String replyText = fullText.substring(0, Math.min(fullText.length(), 40)) + (fullText.length() > 40 ? "..." : "");

            Label replyLabel = new Label("↑ Răspuns la " + senderName + ": " + replyText);
            replyLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-style: italic; -fx-font-size: 11px; -fx-padding: 0 5 0 5;", replyTextColor));

            // Asigurăm că și eticheta de reply respectă alinierea
            replyLabel.setMaxWidth(Double.MAX_VALUE);
            replyLabel.setAlignment(internalAlignment);

            bubbleContent.getChildren().add(replyLabel);
        }

        // --- 4. Continutul Mesajului Principal ---
        Label contentLabel = new Label(msg.getMessage());
        contentLabel.setWrapText(true);
        contentLabel.setStyle(
                String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 8px 12px; -fx-border-radius: 15px; -fx-background-radius: 15px;",
                        bubbleColor, textColor)
        );

        bubbleContent.getChildren().add(contentLabel);

        // --- 5. Containerul de Aliniere (HBox) ---
        HBox bubbleContainer = new HBox(bubbleContent);
        bubbleContainer.setAlignment(alignment);
        bubbleContainer.setStyle("-fx-padding: 2 10 2 10;");

        setGraphic(bubbleContainer);
    }
}