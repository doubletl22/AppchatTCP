package com.example.chatclient.controller;

import com.example.chatclient.model.ChatMessage;
import com.example.chatclient.service.WebSocketService;
import com.example.chatclient.view.MessageCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ChatController {

    @FXML private ListView<ChatMessage> messageListView;
    @FXML private TextField messageInput;

    private WebSocketService webSocketService;
    private String currentUser;
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();

    public void init(String username, WebSocketService service) {
        this.currentUser = username;
        this.webSocketService = service;
        messageListView.setItems(messages);
        messageListView.setCellFactory(param -> new MessageCell(currentUser));
        webSocketService.connect();
    }

    @FXML
    protected void handleSendMessage() {
        String body = messageInput.getText().trim();
        if (body.isEmpty()) return;

        if (body.startsWith("/")) {
            handleCommand(body);
        } else {
            ChatMessage msg = new ChatMessage();
            msg.setType(ChatMessage.MessageType.MESSAGE);
            msg.setFrom(currentUser);
            msg.setBody(body);
            msg.setRoom("global"); // Hardcoded for now
            webSocketService.sendMessage(msg);
        }
        messageInput.clear();
    }

    private void handleCommand(String command) {
        if (command.equalsIgnoreCase("/clear")) {
            messages.clear();
        } else if (command.toLowerCase().startsWith("/join ")) {
            // ... join room logic
        } else if (command.toLowerCase().startsWith("/leave")) {
            // ... leave room logic
        } else {
             ChatMessage systemMessage = new ChatMessage();
            systemMessage.setType(ChatMessage.MessageType.SYSTEM);
            systemMessage.setBody("Unknown command: " + command);
            addMessage(systemMessage);
        }
    }
    
    public void addMessage(ChatMessage message) {
        Platform.runLater(() -> {
            messages.add(message);
            messageListView.scrollTo(messages.size() - 1);
        });
    }
}