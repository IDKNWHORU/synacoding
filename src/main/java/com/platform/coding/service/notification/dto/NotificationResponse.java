package com.platform.coding.service.notification.dto;

import com.platform.coding.domain.notification.Notification;
import lombok.Builder;

import java.time.Instant;

public record NotificationResponse(
        Long notificationId,
        String content,
        String linkUrl,
        boolean isRead,
        Instant createdAt
) {
    @Builder
    public NotificationResponse {}

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .content(notification.getContent())
                .linkUrl(notification.getLinkUrl())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
