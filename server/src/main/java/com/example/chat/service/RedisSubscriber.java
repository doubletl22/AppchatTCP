package com.example.chat.service;

import com.example.chat.model.ChatMessage;
import com.example.chat.ws.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
@Slf4j
public class RedisSubscriber {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public void onMessage(String message) {
        try {
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
            log.debug("Received message from Redis: {}", chatMessage);

            // Broadcast to all connected clients on this instance
            for (WebSocketSession session : ChatWebSocketHandler.sessions.values()) {
                if (session.isOpen()) {
                    // Simple logic: send to everyone. Can be enhanced to target specific rooms/users.
                    // A more advanced implementation would check if the session's user should receive this message.
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(chatMessage)));
                }
            }
        } catch (Exception e) {
            log.error("Could not process Redis message", e);
        }
    }
}