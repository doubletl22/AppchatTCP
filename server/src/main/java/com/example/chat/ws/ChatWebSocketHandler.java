package com.example.chat.ws;

import com.example.chat.model.ChatMessage;
import com.example.chat.model.Message;
import com.example.chat.repo.MessageRepository;
import com.example.chat.service.PresenceService;
import com.example.chat.service.RedisPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    public static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final RedisPublisher redisPublisher;
    private final MessageRepository messageRepository;
    private final PresenceService presenceService;
    private final RateLimiterService rateLimiterService;

    public ChatWebSocketHandler(RedisPublisher redisPublisher, MessageRepository messageRepository, PresenceService presenceService, RateLimiterService rateLimiterService) {
        this.redisPublisher = redisPublisher;
        this.messageRepository = messageRepository;
        this.presenceService = presenceService;
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Missing username attribute"));
            return;
        }
        sessions.put(session.getId(), session);
        presenceService.userOnline(username, "global"); // Default room
        log.info("Session established: {}, user: {}", session.getId(), username);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String username = (String) session.getAttributes().get("username");

        if (!rateLimiterService.tryConsume(session.getId())) {
            sendSystemMessage(session, "Rate limit exceeded. Please slow down.");
            return;
        }

        try {
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            chatMessage.setFrom(username);
            chatMessage.setTimestamp(Instant.now());

            // Persist and publish
            persistAndPublish(chatMessage);

        } catch (Exception e) {
            log.error("Error handling message from {}: {}", username, e.getMessage());
        }
    }

    private void persistAndPublish(ChatMessage chatMessage) {
        // Persist message to PostgreSQL
        if (chatMessage.getType() == ChatMessage.MessageType.MESSAGE) {
            Message dbMessage = new Message();
            dbMessage.setFromUser(chatMessage.getFrom());
            dbMessage.setToUser(chatMessage.getTo());
            dbMessage.setRoom(chatMessage.getRoom() != null ? chatMessage.getRoom() : "global");
            dbMessage.setBody(chatMessage.getBody());
            dbMessage.setCreatedAt(chatMessage.getTimestamp());
            messageRepository.save(dbMessage);
        }

        // Publish to Redis for other server instances
        redisPublisher.publish(chatMessage);
    }
    
    private void sendSystemMessage(WebSocketSession session, String body) {
        try {
            ChatMessage systemMessage = new ChatMessage(
                    ChatMessage.MessageType.SYSTEM, "System", null, "global", body, Instant.now()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(systemMessage)));
        } catch (IOException e) {
            log.error("Failed to send system message to session {}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            presenceService.userOffline(username, "global");
        }
        sessions.remove(session.getId());
        rateLimiterService.removeLimiter(session.getId());
        log.info("Session closed: {}, user: {}, status: {}", session.getId(), username, status);
    }
}