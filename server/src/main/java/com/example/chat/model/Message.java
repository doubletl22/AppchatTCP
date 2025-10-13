package com.example.chat.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "messages")
@Data
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String room;

    @Column(name = "from_user", nullable = false)
    private String fromUser;

    @Column(name = "to_user")
    private String toUser;

    @Column(nullable = false)
    private String body;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();
}