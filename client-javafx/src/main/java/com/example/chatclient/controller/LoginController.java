package com.example.chatclient.controller;

import com.example.chatclient.MainApp;
import com.example.chatclient.service.AuthService;
import com.example.chatclient.service.WebSocketService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    
    private final AuthService authService = new AuthService();

    @FXML
    protected void handleLogin() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            statusLabel.setText("Username cannot be empty.");
            return;
        }

        loginButton.setDisable(true);
        statusLabel.setText("Logging in...");

        // Run login in a background thread to avoid freezing UI
        new Thread(() -> {
            try {
                String token = authService.login(username);
                Platform.runLater(() -> showChatWindow(username, token));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Login failed: " + e.getMessage());
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    private void showChatWindow(String username, String token) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/ChatView.fxml"));
            Parent root = loader.load();
            
            ChatController chatController = loader.getController();
            WebSocketService webSocketService = new WebSocketService(token, chatController);
            chatController.init(username, webSocketService);
            
            Stage stage = new Stage();
            stage.setTitle("Chat - " + username);
            Scene scene = new Scene(root);
            scene.getStylesheets().addAll(
                getClass().getResource("/css/dark-theme.css").toExternalForm(),
                getClass().getResource("/css/chat-bubbles.css").toExternalForm()
            );
            stage.setScene(scene);
            
            stage.setOnCloseRequest(event -> webSocketService.disconnect());
            
            stage.show();
            
            // Close login window
            ((Stage) loginButton.getScene().getWindow()).close();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Failed to load chat window.");
        }
    }
}