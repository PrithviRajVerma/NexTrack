package com.nextrack.notification.api.dto.response;

import java.util.List;

public record NotificationListResponse(
        long unreadCount,
        List<NotificationResponse> notifications,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
