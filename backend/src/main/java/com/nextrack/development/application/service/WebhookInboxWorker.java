package com.nextrack.development.application.service;

import com.nextrack.development.domain.entity.WebhookInboxEvent;
import com.nextrack.development.domain.entity.WebhookInboxStatus;
import com.nextrack.development.domain.repository.WebhookInboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * 2-Phase Webhook Inbox Worker utilizing PostgreSQL SKIP LOCKED row-level claiming
 * and a 5-minute lease lifecycle with poison-pill defense.
 */
@Service
public class WebhookInboxWorker {

    private static final Logger log = LoggerFactory.getLogger(WebhookInboxWorker.class);
    private static final int BATCH_SIZE = 25;
    private static final int MAX_RETRIES = 5;

    private final WebhookInboxEventRepository webhookInboxEventRepository;
    private final WebhookEventProcessor webhookEventProcessor;

    public WebhookInboxWorker(
            WebhookInboxEventRepository webhookInboxEventRepository,
            WebhookEventProcessor webhookEventProcessor
    ) {
        this.webhookInboxEventRepository = webhookInboxEventRepository;
        this.webhookEventProcessor = webhookEventProcessor;
    }

    @Scheduled(fixedDelayString = "${nextrack.integrations.github.webhook.worker-delay-ms:500}")
    public int processPendingEvents() {
        return processBatchOnce();
    }

    /**
     * Executes one claim-and-process iteration cycle.
     */
    public int processBatchOnce() {
        // Phase 1: Claim Batch with SKIP LOCKED in an immediate transaction
        ClaimedBatch batch = claimPendingBatch();
        if (batch == null || batch.events().isEmpty()) {
            return 0;
        }

        int processedCount = 0;

        // Phase 2: Process each event in its own isolated transaction
        for (WebhookInboxEvent event : batch.events()) {
            try {
                webhookEventProcessor.processEventWithLeaseCheck(event, batch.leaseOwnerToken());
                processedCount++;
            } catch (StaleLeaseException sle) {
                log.warn("Lease expired or revoked during processing for event id={}: {}",
                        event.getId(), sle.getMessage());
            } catch (Exception ex) {
                log.warn("Failed to process claimed webhook event id={}: {}", event.getId(), ex.getMessage());
                handleProcessingFailure(event, batch.leaseOwnerToken(), ex);
            }
        }

        return processedCount;
    }

    @Transactional
    public ClaimedBatch claimPendingBatch() {
        Instant now = Instant.now();
        List<WebhookInboxEvent> pending = webhookInboxEventRepository.findPendingEventsForUpdateSkipLocked(now, BATCH_SIZE);
        if (pending.isEmpty()) {
            return null;
        }

        UUID leaseOwnerToken = UUID.randomUUID();
        Instant leaseExpiresAt = now.plus(5, ChronoUnit.MINUTES);
        List<UUID> ids = pending.stream().map(WebhookInboxEvent::getId).toList();

        webhookInboxEventRepository.claimEvents(ids, WebhookInboxStatus.PROCESSING, leaseOwnerToken, leaseExpiresAt);
        log.debug("Claimed batch of {} webhook events with leaseOwnerToken={}", ids.size(), leaseOwnerToken);

        return new ClaimedBatch(pending, leaseOwnerToken);
    }

    @Transactional
    public void handleProcessingFailure(WebhookInboxEvent event, UUID leaseOwnerToken, Exception ex) {
        int nextRetryCount = event.getRetryCount() + 1;
        String errorMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();

        if (nextRetryCount >= MAX_RETRIES) {
            log.error("Webhook event id={} reached max retries ({}). Marking as FAILED.",
                    event.getId(), MAX_RETRIES);
            webhookInboxEventRepository.recordProcessingFailure(
                    event.getId(),
                    leaseOwnerToken,
                    WebhookInboxStatus.FAILED,
                    nextRetryCount,
                    null,
                    errorMessage
            );
        } else {
            long delaySeconds = (long) Math.pow(2, nextRetryCount) + (long) (Math.random() * 2);
            Instant nextRetryAt = Instant.now().plusSeconds(delaySeconds);
            log.info("Scheduling retry #{} for webhook event id={} at {}", nextRetryCount, event.getId(), nextRetryAt);

            webhookInboxEventRepository.recordProcessingFailure(
                    event.getId(),
                    leaseOwnerToken,
                    WebhookInboxStatus.PENDING,
                    nextRetryCount,
                    nextRetryAt,
                    errorMessage
            );
        }
    }

    public record ClaimedBatch(List<WebhookInboxEvent> events, UUID leaseOwnerToken) {}
}
