package com.platform.coding.service.notification;

import com.platform.coding.domain.notification.Notification;
import com.platform.coding.domain.notification.NotificationRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    /**
     * 현재 로그인한 사용자의 알림 목록을 조회합니다.
     * @param user 현재 사용자
     * @param pageable 페이지 정보
     * @return 알림 목록 DTO 페이지
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(User user, Pageable pageable) {
        return notificationRepository.findAllByUserOrderByCreatedAtDesc(user, pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));

        // 자신의 알림만 읽을 수 있도록 권한 확인
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("자신의 알림만 읽음 처리할 수 있습니다.");
        }

        notification.markAsRead();
    }

    /**
     * 현재 로그인한 사용자의 모든 알림을 '읽음'으로 표시한다.
     * @param user 현재 사용자
     */
    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadByUser(user);
    }

    @Transactional
    public void createNotification(User user, String content, String linkUrl) {
        Notification notification = Notification.builder()
                .user(user)
                .content(content)
                .linkUrl(linkUrl)
                .build();
        notificationRepository.save(notification);
    }

    /**
     * 사용자의 읽지 않은 알림 개수를 조회한다.
     * @param user 현재 사용자
     * @return 읽지 않은 알림 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(User user) {
        if (user == null) return 0;
        return notificationRepository.countByUserAndIsReadFalse(user);
    }
}
