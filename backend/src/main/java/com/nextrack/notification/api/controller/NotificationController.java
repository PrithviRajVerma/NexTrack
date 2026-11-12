package com.nextrack.notification.api.controller;

import com.nextrack.notification.api.dto.response.NotificationListResponse;
import com.nextrack.notification.api.dto.response.NotificationResponse;
import com.nextrack.notification.application.service.NotificationService;
import com.nextrack.notification.domain.entity.Notification;
import com.nextrack.shared.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        int pageSize = Math.min(pageable.getPageSize() <= 0 ? DEFAULT_PAGE_SIZE : pageable.getPageSize(), MAX_PAGE_SIZE);
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        Pageable bounded = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notification> page = notificationService.getUserNotifications(principal.id(), bounded);
        long unreadCount = notificationService.getUnreadCount(principal.id());

        List<NotificationResponse> list = page.getContent().stream()
                .map(NotificationResponse::from)
                .toList();

        NotificationListResponse response = new NotificationListResponse(
                unreadCount,
                list,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Notification notification = notificationService.markAsRead(id, principal.id());
        return ResponseEntity.ok(NotificationResponse.from(notification));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.markAllAsRead(principal.id());
        return ResponseEntity.noContent().build();
    }
}
