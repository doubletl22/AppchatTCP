package com.example.chat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private MessageType type;
    private String from;
    private String to;
    private String room;
    private String body;
    private Instant timestamp;

    public enum MessageType {
        MESSAGE, JOIN, LEAVE, SYSTEM, PRESENCE, HISTORY
    }
}