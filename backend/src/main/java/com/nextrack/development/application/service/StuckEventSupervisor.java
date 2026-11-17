package com.nextrack.development.application.service;

import com.nextrack.development.domain.entity.WebhookInboxEvent;
import com.nextrack.development.domain.repository.WebhookInboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Background supervisor task scanning for webhook events stuck in PROCESSING past their lease
 * expiration and resetting them to PENDING for re-claiming.
 */
@Service
public class StuckEventSupervisor {

    private static final Logger log = LoggerFactory.getLogger(StuckEventSupervisor.class);
    private static final int SCAN_LIMIT = 100;

    private final WebhookInboxEventRepository webhookInboxEventRepository;

    public StuckEventSupervisor(WebhookInboxEventRepository webhookInboxEventRepository) {
        this.webhookInboxEventRepository = webhookInboxEventRepository;
    }

    @Scheduled(fixedDelayString = "${nextrack.integrations.github.webhook.supervisor-delay-ms:60000}")
    @Transactional
    public int scanAndRecoverStuckEvents() {
        Instant now = Instant.now();
        List<WebhookInboxEvent> stuck = webhookInboxEventRepository.findStuckProcessingEvents(now, SCAN_LIMIT);
        if (stuck.isEmpty()) {
            return 0;
        }

        List<UUID> ids = stuck.stream().map(WebhookInboxEvent::getId).toList();
        int resetCount = webhookInboxEventRepository.resetStuckEvents(ids);
        log.warn("Recovered {} stuck webhook events whose processing lease expired past {}", resetCount, now);

        return resetCount;
    }
}
