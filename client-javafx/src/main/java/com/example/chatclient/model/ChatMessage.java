package com.example.chatclient.model;

import java.time.Instant;

public class ChatMessage {
    private MessageType type;
    private String from;
    private String to;
    private String room;
    private String body;
    private Instant timestamp;

    // Getters and Setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public enum MessageType {
        MESSAGE, JOIN, LEAVE, SYSTEM, PRESENCE, HISTORY
    }
}