package com.chat.client;

import com.chat.common.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.Optional;

public class ChatApplication extends Application {

    private final TextArea chatArea = new TextArea();
    private final TextField messageField = new TextField();
    private ClientService clientService;
    private String username;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Yêu cầu người dùng nhập tên trước khi vào chat
        promptForUsername();
        if (username == null || username.trim().isEmpty()) {
            System.out.println("Tên người dùng không hợp lệ. Thoát ứng dụng.");
            Platform.exit();
            return;
        }

        primaryStage.setTitle("Chat Client - " + username);

        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        messageField.setPromptText("Nhập tin nhắn...");
        Button sendButton = new Button("Gửi");
        sendButton.setDefaultButton(true);

        sendButton.setOnAction(e -> sendMessage());
        messageField.setOnAction(e -> sendMessage());

        HBox inputBox = new HBox(10, messageField, sendButton);
        inputBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(chatArea);
        root.setBottom(inputBox);

        Scene scene = new Scene(root, 450, 600);
        // Load file CSS
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();

        setupClient();
    }

    private void promptForUsername() {
        TextInputDialog dialog = new TextInputDialog("User");
        dialog.setTitle("Tên người dùng");
        dialog.setHeaderText("Chào mừng bạn đến với phòng chat!");
        dialog.setContentText("Vui lòng nhập tên của bạn:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> username = name);
    }

    private void setupClient() {
        // Callback để xử lý khi nhận được tin nhắn
        clientService = new ClientService(message -> {
            Platform.runLater(() -> {
                chatArea.appendText(message.sender() + ": " + message.content() + "\n");
            });
        });

        // Kết nối đến server trên một luồng mới
        new Thread(clientService::connect).start();
    }

    private void sendMessage() {
        String content = messageField.getText();
        if (content != null && !content.trim().isEmpty()) {
            Message message = new Message(username, content);
            clientService.sendMessage(message);
            messageField.clear();
        }
    }
}
