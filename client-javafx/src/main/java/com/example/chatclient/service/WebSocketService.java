package com.example.chatclient.service;

import com.example.chatclient.controller.ChatController;
import com.example.chatclient.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketService {

    private WebSocketClient client;
    private final String token;
    private final ChatController chatController;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String WEBSOCKET_URL = "ws://localhost:8080/chat?token=";

    public WebSocketService(String token, ChatController chatController) {
        this.token = token;
        this.chatController = chatController;
    }

    public void connect() {
        try {
            client = new WebSocketClient(new URI(WEBSOCKET_URL + token)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("Connected to WebSocket server");
                }

                @Override
                public void onMessage(String message) {
                    try {
                        ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
                        chatController.addMessage(chatMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Disconnected from WebSocket server: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };
            client.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(ChatMessage message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            client.send(jsonMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (client != null) {
            client.close();
        }
    }
}