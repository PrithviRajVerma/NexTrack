package com.nextrack.development.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.development.api.dto.WebhookIngestionResponse;
import com.nextrack.development.application.service.WebhookIngestionService;
import com.nextrack.shared.api.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integrations/github")
public class WebhookIngestionController {

    private final WebhookIngestionService webhookIngestionService;
    private final ObjectMapper objectMapper;

    public WebhookIngestionController(
            WebhookIngestionService webhookIngestionService,
            ObjectMapper objectMapper
    ) {
        this.webhookIngestionService = webhookIngestionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/webhooks", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> ingestWebhook(
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestHeader(value = "X-GitHub-Event", required = false, defaultValue = "ping") String eventType,
            @RequestBody String rawPayload,
            HttpServletRequest request
    ) {
        if (deliveryId == null || deliveryId.isBlank()) {
            ApiErrorResponse error = ApiErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(),
                    "Missing Delivery ID",
                    "Header X-GitHub-Delivery is required for webhook tracking.",
                    "MISSING_DELIVERY_ID",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        // Validate that body is structurally valid JSON prior to database insertion
        try {
            objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException jpe) {
            ApiErrorResponse error = ApiErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(),
                    "Malformed Webhook JSON",
                    "Payload must be valid JSON: " + jpe.getOriginalMessage(),
                    "MALFORMED_WEBHOOK_JSON",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        WebhookIngestionResponse response = webhookIngestionService.ingestEvent(
                "GITHUB",
                deliveryId,
                eventType,
                rawPayload
        );

        if ("DUPLICATE".equalsIgnoreCase(response.status())) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
