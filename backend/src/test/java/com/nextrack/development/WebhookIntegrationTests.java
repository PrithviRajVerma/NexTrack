package com.nextrack.development;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.development.application.service.StuckEventSupervisor;
import com.nextrack.development.application.service.WebhookInboxWorker;
import com.nextrack.development.domain.entity.WebhookInboxEvent;
import com.nextrack.development.domain.entity.WebhookInboxStatus;
import com.nextrack.development.domain.repository.WebhookInboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WebhookIntegrationTests {

    private static final String TEST_SECRET = "integration_test_webhook_secret_key";
    private static final String WEBHOOK_URI = "/api/v1/integrations/github/webhooks";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nextrack_webhook_test")
            .withUsername("webhook_user")
            .withPassword("webhook_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "25");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("nextrack.integrations.github.webhook.secret", () -> TEST_SECRET);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebhookInboxEventRepository webhookInboxEventRepository;

    @Autowired
    private WebhookInboxWorker webhookInboxWorker;

    @Autowired
    private StuckEventSupervisor stuckEventSupervisor;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        webhookInboxEventRepository.deleteAll();
    }

    private String computeHmacSignature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Should ingest valid signed webhook and return 202 Accepted in sub-30ms target")
    void shouldIngestValidSignedWebhook() throws Exception {
        String deliveryId = "deliv-" + UUID.randomUUID();
        String payload = """
            {
                "action": "opened",
                "zen": "Encourage branching.",
                "repository": {
                    "full_name": "acme/backend"
                }
            }
            """;
        String signature = computeHmacSignature(payload);

        mockMvc.perform(post(WEBHOOK_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-Hub-Signature-256", signature)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.deliveryId").value(deliveryId))
                .andExpect(jsonPath("$.eventId").isNotEmpty());

        List<WebhookInboxEvent> events = webhookInboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        WebhookInboxEvent saved = events.get(0);
        assertThat(saved.getDeliveryId()).isEqualTo(deliveryId);
        assertThat(saved.getEventType()).isEqualTo("pull_request");
        assertThat(saved.getStatus()).isEqualTo(WebhookInboxStatus.PENDING);
    }

    @Test
    @DisplayName("Should reject forged signature with 401 Unauthorized and create 0 database records")
    void shouldRejectForgedSignature() throws Exception {
        String deliveryId = "deliv-forged-" + UUID.randomUUID();
        String payload = "{\"action\": \"ping\"}";
        String forgedSignature = "sha256=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

        mockMvc.perform(post(WEBHOOK_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-GitHub-Event", "ping")
                        .header("X-Hub-Signature-256", forgedSignature)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_WEBHOOK_SIGNATURE"));

        assertThat(webhookInboxEventRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should reject malformed JSON with 400 Bad Request and create 0 database records")
    void shouldRejectMalformedJson() throws Exception {
        String deliveryId = "deliv-malformed-" + UUID.randomUUID();
        String malformedPayload = "{ this is not valid JSON : ";
        String signature = computeHmacSignature(malformedPayload);

        mockMvc.perform(post(WEBHOOK_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", signature)
                        .content(malformedPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_WEBHOOK_JSON"));

        assertThat(webhookInboxEventRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should acknowledge duplicate delivery idempotently with 200 OK and single DB record")
    void shouldAcknowledgeDuplicateDeliveryIdempotently() throws Exception {
        String deliveryId = "deliv-dup-" + UUID.randomUUID();
        String payload = "{\"action\": \"ping\", \"zen\": \"Practicality beats purity.\"}";
        String signature = computeHmacSignature(payload);

        // First delivery: Accepted
        mockMvc.perform(post(WEBHOOK_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-GitHub-Event", "ping")
                        .header("X-Hub-Signature-256", signature)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Second delivery (duplicate): 200 OK
        mockMvc.perform(post(WEBHOOK_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Delivery", deliveryId)
                        .header("X-GitHub-Event", "ping")
                        .header("X-Hub-Signature-256", signature)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"));

        List<WebhookInboxEvent> events = webhookInboxEventRepository.findAll();
        assertThat(events).hasSize(1);
    }

    @Test
    @DisplayName("Should claim pending event, verify lease token, and transition to PROCESSED")
    void shouldClaimAndProcessEventWithLease() {
        WebhookInboxEvent event = new WebhookInboxEvent(
                "GITHUB",
                "deliv-claim-" + UUID.randomUUID(),
                "push",
                "{\"ref\": \"refs/heads/feature/NT-101\"}"
        );
        webhookInboxEventRepository.save(event);

        int processed = webhookInboxWorker.processBatchOnce();
        assertThat(processed).isEqualTo(1);

        WebhookInboxEvent updated = webhookInboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(WebhookInboxStatus.PROCESSED);
        assertThat(updated.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should defend against poison pill by retrying 5 times and transitioning to FAILED")
    void shouldHandlePoisonPillWithRetriesAndFail() {
        WebhookInboxEvent poisonEvent = new WebhookInboxEvent(
                "GITHUB",
                "deliv-poison-" + UUID.randomUUID(),
                "push",
                "{\"simulate_failure\": true}"
        );
        // Set retryCount to 4 so next execution triggers 5th attempt and marks FAILED
        poisonEvent.setRetryCount(4);
        webhookInboxEventRepository.save(poisonEvent);

        webhookInboxWorker.processBatchOnce();

        WebhookInboxEvent updated = webhookInboxEventRepository.findById(poisonEvent.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(WebhookInboxStatus.FAILED);
        assertThat(updated.getRetryCount()).isEqualTo(5);
        assertThat(updated.getErrorMessage()).contains("Simulated processing failure");
    }

    @Test
    @DisplayName("Supervisor should recover stuck PROCESSING event past lease expiration back to PENDING")
    void shouldRecoverStuckLeaseEvent() {
        WebhookInboxEvent stuckEvent = new WebhookInboxEvent(
                "GITHUB",
                "deliv-stuck-" + UUID.randomUUID(),
                "pull_request",
                "{\"action\": \"opened\"}"
        );
        stuckEvent.setStatus(WebhookInboxStatus.PROCESSING);
        stuckEvent.setLeaseOwnerToken(UUID.randomUUID());
        // Lease expired 10 minutes ago
        stuckEvent.setProcessingLeaseExpiresAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        webhookInboxEventRepository.save(stuckEvent);

        int recovered = stuckEventSupervisor.scanAndRecoverStuckEvents();
        assertThat(recovered).isEqualTo(1);

        WebhookInboxEvent recoveredEvent = webhookInboxEventRepository.findById(stuckEvent.getId()).orElseThrow();
        assertThat(recoveredEvent.getStatus()).isEqualTo(WebhookInboxStatus.PENDING);
        assertThat(recoveredEvent.getLeaseOwnerToken()).isNull();
        assertThat(recoveredEvent.getProcessingLeaseExpiresAt()).isNull();
    }
}
