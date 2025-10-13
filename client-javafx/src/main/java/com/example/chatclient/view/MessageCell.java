package com.example.chatclient.view;

import com.example.chatclient.model.ChatMessage;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.time.format.DateTimeFormatter;

public class MessageCell extends ListCell<ChatMessage> {

    private final HBox container = new HBox();
    private final VBox bubble = new VBox();
    private final Text messageText = new Text();
    private final Label infoLabel = new Label();
    private final String currentUser;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public MessageCell(String currentUser) {
        this.currentUser = currentUser;
        bubble.getChildren().addAll(messageText, infoLabel);
        container.getChildren().add(bubble);
        messageText.setWrappingWidth(350); // Prevent long messages from being too wide
    }

    @Override
    protected void updateItem(ChatMessage message, boolean empty) {
        super.updateItem(message, empty);
        if (empty || message == null) {
            setText(null);
            setGraphic(null);
        } else {
            messageText.setText(message.getBody());
            String info = message.getFrom() + " at " + (message.getTimestamp() != null ? formatter.format(message.getTimestamp().atZone(java.time.ZoneId.systemDefault())) : "");
            infoLabel.setText(info);
            infoLabel.getStyleClass().add("timestamp-label");
            
            boolean isSelf = currentUser.equals(message.getFrom());
            if (isSelf) {
                container.setAlignment(Pos.CENTER_RIGHT);
                bubble.getStyleClass().setAll("chat-bubble", "self");
            } else {
                container.setAlignment(Pos.CENTER_LEFT);
                bubble.getStyleClass().setAll("chat-bubble", "other");
            }
            setGraphic(container);
        }
    }
}