package com.nextrack.development.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "webhook_inbox_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_webhook_provider_delivery", columnNames = {"provider", "delivery_id"})
        }
)
public class WebhookInboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32)
    private String provider = "GITHUB";

    @Column(name = "delivery_id", nullable = false, length = 128)
    private String deliveryId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WebhookInboxStatus status = WebhookInboxStatus.PENDING;

    @Column(name = "lease_owner_token")
    private UUID leaseOwnerToken;

    @Column(name = "processing_lease_expires_at")
    private Instant processingLeaseExpiresAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    protected WebhookInboxEvent() {}

    public WebhookInboxEvent(String provider, String deliveryId, String eventType, String payload) {
        this.provider = provider != null ? provider : "GITHUB";
        this.deliveryId = deliveryId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = WebhookInboxStatus.PENDING;
        this.retryCount = 0;
        this.receivedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public WebhookInboxStatus getStatus() {
        return status;
    }

    public void setStatus(WebhookInboxStatus status) {
        this.status = status;
    }

    public UUID getLeaseOwnerToken() {
        return leaseOwnerToken;
    }

    public void setLeaseOwnerToken(UUID leaseOwnerToken) {
        this.leaseOwnerToken = leaseOwnerToken;
    }

    public Instant getProcessingLeaseExpiresAt() {
        return processingLeaseExpiresAt;
    }

    public void setProcessingLeaseExpiresAt(Instant processingLeaseExpiresAt) {
        this.processingLeaseExpiresAt = processingLeaseExpiresAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
