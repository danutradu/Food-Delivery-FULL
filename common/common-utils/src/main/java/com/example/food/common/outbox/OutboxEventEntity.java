package com.example.food.common.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@Getter
@Setter
public class OutboxEventEntity {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String topic;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private String key;
    @Lob
    @Column(nullable = false)
    private String payloadJson;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private boolean published = false;
    @Column(nullable = false)
    private int retryCount = 0;
    private Instant lastRetryAt;
    private String lastError;
    private Instant publishedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;
}
