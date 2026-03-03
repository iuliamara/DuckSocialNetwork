package com.ubb.gui;

import com.ubb.domain.*;
import com.ubb.service.FriendshipServiceInterface;
import com.ubb.service.MessageServiceInterface;
import com.ubb.service.exceptions.ServiceException;
import com.ubb.utils.MessageCell;
import com.ubb.utils.UserCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox; // Import necesar pentru inputArea
import java.util.Collections;
import java.util.List;

public class ChatController implements Observer<Message> {

    @FXML private ListView<User> contactListView;
    @FXML private ListView<Message> chatListView;
    @FXML private TextField messageInputField;
    @FXML private Label conversationTitleLabel;
    @FXML private Button cancelReplyButton;
    @FXML private VBox inputArea;

    private MessageServiceInterface messageService;
    private FriendshipServiceInterface friendshipService;
    private User authenticatedUser;
    private User currentRecipient;

    private final ObservableList<Message> conversationData = FXCollections.observableArrayList();
    private Message replyToMessage = null;
    private boolean isSubscribed = false;

    public void setContext(MessageServiceInterface messageService,
                           FriendshipServiceInterface friendshipService,
                           User authenticatedUser) {
        this.messageService = messageService;
        this.friendshipService = friendshipService;
        this.authenticatedUser = authenticatedUser;

        if (!isSubscribed && messageService instanceof Subject) {
            ((Subject<Message>) messageService).addObserver(this);
            isSubscribed = true;
        }

        initializeChatLogic();
    }

    @FXML
    public void initialize() {
        chatListView.setItems(conversationData);

        // Initial, zona de input este ascunsa complet
        if (inputArea != null) {
            inputArea.setVisible(false);
            inputArea.setManaged(false);
        }
    }

    private void initializeChatLogic() {
        contactListView.setPlaceholder(new Label("Se incarca prietenii..."));

        Thread loaderThread = new Thread(() -> {
            try {
                List<User> friends = friendshipService.getAllFriends(authenticatedUser.getId());
                ObservableList<User> userList = FXCollections.observableArrayList(friends);

                Platform.runLater(() -> {
                    contactListView.setItems(userList);
                    contactListView.setCellFactory(listView -> new UserCell(messageService, authenticatedUser.getId()));
                    chatListView.setCellFactory(listView -> new MessageCell(authenticatedUser));

                    // Logica de selectie cu gestionarea vizibilitatii inputArea
                    contactListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal != null) {
                            currentRecipient = newVal;
                            conversationTitleLabel.setText("Conversatie cu: " + currentRecipient.getUsername());

                            // Zona de input devine vizibila si activa in layout
                            if (inputArea != null) {
                                inputArea.setVisible(true);
                                inputArea.setManaged(true);
                            }

                            messageService.markConversationAsRead(authenticatedUser.getId(), currentRecipient.getId());
                            loadConversation();
                            resetReplyState();
                        } else {
                            // Daca se pierde selectia, zona de input dispare
                            if (inputArea != null) {
                                inputArea.setVisible(false);
                                inputArea.setManaged(false);
                            }
                            conversationTitleLabel.setText("Selecteaza o conversatie");
                        }
                    });

                    if (userList.isEmpty()) {
                        contactListView.setPlaceholder(new Label("Nu ai niciun prieten inca."));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private void loadConversation() {
        if (currentRecipient == null) return;

        Thread loader = new Thread(() -> {
            try {
                List<Message> messages = messageService.getConversation(authenticatedUser.getId(), currentRecipient.getId());
                Platform.runLater(() -> {
                    conversationData.setAll(messages);
                    if (!conversationData.isEmpty()) {
                        chatListView.scrollTo(conversationData.size() - 1);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        loader.setDaemon(true);
        loader.start();
    }

    @Override
    public void update(Subject<Message> subject, Message newMessage) {
        Platform.runLater(() -> contactListView.refresh());

        if (newMessage == null || currentRecipient == null) return;

        Long senderId = newMessage.getFrom().getId();
        Long recipientId = currentRecipient.getId();
        Long authId = authenticatedUser.getId();

        boolean fromMeToHim = senderId.equals(authId) && newMessage.getTo().stream().anyMatch(u -> u.getId().equals(recipientId));
        boolean fromHimToMe = senderId.equals(recipientId) && newMessage.getTo().stream().anyMatch(u -> u.getId().equals(authId));

        if (fromMeToHim || fromHimToMe) {
            Platform.runLater(() -> {
                conversationData.add(newMessage);
                chatListView.scrollTo(conversationData.size() - 1);

                if (fromHimToMe) {
                    messageService.markConversationAsRead(authId, recipientId);
                }
            });
        }
    }

    @FXML
    private void handleSendMessage() {
        String content = messageInputField.getText().trim();
        if (currentRecipient == null || content.isEmpty()) return;

        // 1. Salvam referintele necesare inainte de a reseta UI-ul
        Long senderId = authenticatedUser.getId();
        List<Long> recipientIds = Collections.singletonList(currentRecipient.getId());
        Message replyTo = this.replyToMessage;

        // 2. Feedback vizual INSTANT: Goleste campul si reseteaza reply-ul
        messageInputField.clear();
        resetReplyState();

        // 3. Apelam metoda asincrona din serviciu
        messageService.sendMessageAsync(senderId, recipientIds, content, replyTo)
                .thenAccept(savedMessage -> {
                    System.out.println("Mesaj trimis cu succes: " + savedMessage.getId());
                })
                .exceptionally(ex -> {
                    // Daca apare o eroare in fundal (ex: rețea cazuta), anunțam utilizatorul pe thread-ul de UI
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Eroare la trimitere", ex.getMessage());
                    });
                    return null;
                });
    }

    @FXML
    private void handleReplyMessage() {
        Message selected = chatListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        this.replyToMessage = selected;
        String snippet = selected.getMessage();
        if (snippet != null && snippet.length() > 20) snippet = snippet.substring(0, 20) + "...";

        messageInputField.setPromptText("Raspuns catre " +
                (selected.getFrom() != null ? selected.getFrom().getUsername() : "User") + ": " + snippet);

        if (cancelReplyButton != null) cancelReplyButton.setVisible(true);
        messageInputField.requestFocus();
    }

    @FXML private void handleCancelReply() { resetReplyState(); }

    @FXML
    private void handleChatListClick() {
        if (chatListView.getSelectionModel().getSelectedItem() == null) {
            resetReplyState();
        }
    }

    private void resetReplyState() {
        this.replyToMessage = null;
        messageInputField.setPromptText("Scrie un mesaj...");
        if (cancelReplyButton != null) cancelReplyButton.setVisible(false);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}