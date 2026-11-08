package com.nextrack.audit.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(nullable = false, length = 64)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_payload", columnDefinition = "jsonb")
    private String beforePayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_payload", columnDefinition = "jsonb")
    private String afterPayload;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AuditEvent() {}

    public AuditEvent(
            UUID actorId,
            String entityType,
            UUID entityId,
            String action,
            String beforePayload,
            String afterPayload,
            String traceId
    ) {
        this.actorId = actorId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.beforePayload = beforePayload;
        this.afterPayload = afterPayload;
        this.traceId = traceId;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getBeforePayload() { return beforePayload; }
    public String getAfterPayload() { return afterPayload; }
    public String getTraceId() { return traceId; }
    public Instant getCreatedAt() { return createdAt; }
}
