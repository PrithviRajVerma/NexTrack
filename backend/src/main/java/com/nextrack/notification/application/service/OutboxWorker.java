package com.nextrack.notification.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.notification.domain.entity.Notification;
import com.nextrack.notification.domain.entity.OutboxEvent;
import com.nextrack.notification.domain.repository.NotificationRepository;
import com.nextrack.notification.domain.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    private static final int BATCH_SIZE = 25;

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public OutboxWorker(
            OutboxEventRepository outboxEventRepository,
            NotificationRepository notificationRepository,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public int processPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findPendingEventsForUpdateSkipLocked(BATCH_SIZE);
        if (pending.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        for (OutboxEvent event : pending) {
            try {
                processSingleEvent(event);
                event.markPublished();
                processedCount++;
            } catch (Exception ex) {
                log.warn("Failed to process outbox event id={}, type={}: {}", event.getId(), event.getEventType(), ex.getMessage());
                long delaySeconds = (long) Math.pow(2, event.getRetryCount()) + (long) (Math.random() * 2);
                event.recordFailure(ex.getMessage(), Instant.now().plusSeconds(delaySeconds));
            }
        }

        outboxEventRepository.saveAll(pending);
        return processedCount;
    }

    private void processSingleEvent(OutboxEvent event) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(
                event.getPayload(),
                new TypeReference<>() {}
        );

        Object recipientObj = payload.get("recipientId");
        if (recipientObj == null) {
            throw new IllegalStateException("Outbox payload missing recipientId");
        }
        UUID recipientId = UUID.fromString(recipientObj.toString());

        String dedupKey = (String) payload.get("dedupKey");
        if (dedupKey == null || dedupKey.isBlank()) {
            dedupKey = "OUTBOX-" + event.getId();
        }

        if (notificationRepository.existsByDedupKey(dedupKey)) {
            log.debug("Notification with dedupKey={} already exists. Skipping duplicate dispatch.", dedupKey);
            return;
        }

        try {
            Notification notification = new Notification(
                    recipientId,
                    event.getEventType(),
                    event.getPayload(),
                    dedupKey
            );
            notificationRepository.save(notification);
        } catch (DataIntegrityViolationException dive) {
            // Concurrently inserted notification with same dedupKey
            log.debug("Concurrent dedup key hit for key={}", dedupKey);
        }
    }
}
