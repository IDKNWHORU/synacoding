package com.platform.coding.controller.notification;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.notification.NotificationService;
import com.platform.coding.service.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
            ) {
        Page<NotificationResponse> notifications = notificationService.getMyNotifications(user, pageable);

        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 개수를 반환하는 API
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(Map.of("count", 0L));
        }
        long count = notificationService.getUnreadNotificationCount(user);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal User user
    ) {
        notificationService.markAsRead(notificationId, user);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user);

        return ResponseEntity.noContent().build();
    }
}
