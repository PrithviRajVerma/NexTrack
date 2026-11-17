package com.nextrack.development.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.development.domain.entity.WebhookInboxEvent;
import com.nextrack.development.domain.entity.WebhookInboxStatus;
import com.nextrack.development.domain.repository.WebhookInboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class WebhookEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventProcessor.class);

    private final WebhookInboxEventRepository webhookInboxEventRepository;
    private final ObjectMapper objectMapper;

    public WebhookEventProcessor(
            WebhookInboxEventRepository webhookInboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.webhookInboxEventRepository = webhookInboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes domain processing for a single webhook event in an isolated transaction.
     * Enforces the 2-phase lease token verification prior to commit.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEventWithLeaseCheck(WebhookInboxEvent event, UUID leaseOwnerToken) {
        log.debug("Processing webhook event: id={}, type={}, deliveryId={}",
                event.getId(), event.getEventType(), event.getDeliveryId());

        // Parse and validate payload structure
        try {
            JsonNode rootNode = objectMapper.readTree(event.getPayload());

            // Check for simulated poison pill in test scenarios or corrupt payload
            if (rootNode.has("simulate_failure") && rootNode.get("simulate_failure").asBoolean()) {
                throw new IllegalStateException("Simulated processing failure (poison pill)");
            }

            dispatchDomainEvent(event.getEventType(), rootNode);

        } catch (Exception ex) {
            log.warn("Domain processing failed for webhook event id={}: {}", event.getId(), ex.getMessage());
            throw new RuntimeException("Domain processing failure: " + ex.getMessage(), ex);
        }

        // Lease Check Before Commit: Verify worker still holds lease token
        int updated = webhookInboxEventRepository.markProcessedWithLeaseCheck(
                event.getId(),
                leaseOwnerToken,
                WebhookInboxStatus.PROCESSED,
                Instant.now()
        );

        if (updated == 0) {
            // Lease expired and was reset or claimed by supervisor/another worker
            throw new StaleLeaseException("Lease was revoked or expired for webhook event " + event.getId());
        }

        log.debug("Successfully processed and committed webhook event id={}", event.getId());
    }

    private void dispatchDomainEvent(String eventType, JsonNode payload) {
        switch (eventType) {
            case "ping" -> log.info("Received GitHub ping event: zen='{}'",
                    payload.has("zen") ? payload.get("zen").asText() : "");
            case "push" -> log.info("Received GitHub push event on ref='{}'",
                    payload.has("ref") ? payload.get("ref").asText() : "");
            case "pull_request" -> log.info("Received GitHub pull_request action='{}'",
                    payload.has("action") ? payload.get("action").asText() : "");
            case "workflow_run" -> log.info("Received GitHub workflow_run action='{}'",
                    payload.has("action") ? payload.get("action").asText() : "");
            case "deployment_status" -> log.info("Received GitHub deployment_status state='{}'",
                    payload.has("deployment_status") ? payload.get("deployment_status").path("state").asText() : "");
            default -> log.info("Received GitHub webhook event type='{}'", eventType);
        }
    }
}
