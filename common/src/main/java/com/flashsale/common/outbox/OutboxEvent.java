package com.flashsale.common.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Mapped superclass / base JPA entity declaring outbox table structure for service schemas.
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(nullable = false, length = 64)
    private String aggregateType;

    @Column(nullable = false, length = 64)
    private String aggregateId;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 128)
    private String destinationTopic;

    @Column(length = 64)
    private String partitionKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(length = 1024)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant processedAt;

    @PrePersist
    protected void onCreate() {
        if (this.eventId == null || this.eventId.isBlank()) {
            this.eventId = UUID.randomUUID().toString();
        }
        if (this.status == null) {
            this.status = OutboxStatus.PENDING;
        }
        this.createdAt = Instant.now();
        this.retryCount = 0;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.processedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markFailed(String error) {
        this.retryCount++;
        this.status = OutboxStatus.FAILED;
        this.errorMessage = (error != null && error.length() > 1020) ? error.substring(0, 1020) : error;
        this.processedAt = Instant.now();
    }
}
