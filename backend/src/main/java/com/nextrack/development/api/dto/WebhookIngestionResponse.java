package com.nextrack.development.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebhookIngestionResponse(
        String status,
        String deliveryId,
        UUID eventId,
        String message
) {
    public static WebhookIngestionResponse accepted(String deliveryId, UUID eventId) {
        return new WebhookIngestionResponse("ACCEPTED", deliveryId, eventId, "Webhook event accepted for asynchronous processing");
    }

    public static WebhookIngestionResponse duplicate(String deliveryId) {
        return new WebhookIngestionResponse("DUPLICATE", deliveryId, null, "Webhook delivery ID already processed or queued");
    }
}
