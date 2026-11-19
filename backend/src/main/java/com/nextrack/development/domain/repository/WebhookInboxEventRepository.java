package com.nextrack.development.domain.repository;

import com.nextrack.development.domain.entity.WebhookInboxEvent;
import com.nextrack.development.domain.entity.WebhookInboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookInboxEventRepository extends JpaRepository<WebhookInboxEvent, UUID> {

    boolean existsByProviderAndDeliveryId(String provider, String deliveryId);

    Optional<WebhookInboxEvent> findByProviderAndDeliveryId(String provider, String deliveryId);

    @Query(value = """
        SELECT * FROM webhook_inbox_events
        WHERE status = 'PENDING'
          AND (next_retry_at IS NULL OR next_retry_at <= :now)
        ORDER BY received_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<WebhookInboxEvent> findPendingEventsForUpdateSkipLocked(@Param("now") Instant now, @Param("limit") int limit);

    @Query(value = """
        SELECT * FROM webhook_inbox_events
        WHERE status = 'PROCESSING'
          AND processing_lease_expires_at < :now
        LIMIT :limit
    """, nativeQuery = true)
    List<WebhookInboxEvent> findStuckProcessingEvents(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
        UPDATE WebhookInboxEvent e
        SET e.status = :targetStatus,
            e.leaseOwnerToken = :leaseOwnerToken,
            e.processingLeaseExpiresAt = :leaseExpiresAt
        WHERE e.id IN :ids
    """)
    int claimEvents(
        @Param("ids") List<UUID> ids,
        @Param("targetStatus") WebhookInboxStatus targetStatus,
        @Param("leaseOwnerToken") UUID leaseOwnerToken,
        @Param("leaseExpiresAt") Instant leaseExpiresAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
        UPDATE WebhookInboxEvent e
        SET e.status = :targetStatus,
            e.processedAt = :now,
            e.errorMessage = null
        WHERE e.id = :id
          AND e.leaseOwnerToken = :leaseOwnerToken
          AND e.status = com.nextrack.development.domain.entity.WebhookInboxStatus.PROCESSING
    """)
    int markProcessedWithLeaseCheck(
        @Param("id") UUID id,
        @Param("leaseOwnerToken") UUID leaseOwnerToken,
        @Param("targetStatus") WebhookInboxStatus targetStatus,
        @Param("now") Instant now
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
        UPDATE WebhookInboxEvent e
        SET e.status = :targetStatus,
            e.retryCount = :retryCount,
            e.nextRetryAt = :nextRetryAt,
            e.errorMessage = :errorMessage,
            e.leaseOwnerToken = null,
            e.processingLeaseExpiresAt = null
        WHERE e.id = :id
          AND e.leaseOwnerToken = :leaseOwnerToken
    """)
    int recordProcessingFailure(
        @Param("id") UUID id,
        @Param("leaseOwnerToken") UUID leaseOwnerToken,
        @Param("targetStatus") WebhookInboxStatus targetStatus,
        @Param("retryCount") int retryCount,
        @Param("nextRetryAt") Instant nextRetryAt,
        @Param("errorMessage") String errorMessage
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("""
        UPDATE WebhookInboxEvent e
        SET e.status = com.nextrack.development.domain.entity.WebhookInboxStatus.PENDING,
            e.leaseOwnerToken = null,
            e.processingLeaseExpiresAt = null
        WHERE e.id IN :ids
    """)
    int resetStuckEvents(@Param("ids") List<UUID> ids);
}
