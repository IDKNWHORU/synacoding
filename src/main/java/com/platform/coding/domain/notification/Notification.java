package com.platform.coding.domain.notification;

import com.platform.coding.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notifications", schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;
    
    // 알림을 받는 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String content;

    // 클릭 시 이동할 URL
    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public Notification(User user, String content, String linkUrl) {
        this.user = user;
        this.content = content;
        this.linkUrl = linkUrl;
        this.createdAt = Instant.now();
    }

    /**
     * 알림을 '읽음' 상태로 변경함.
     */
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
        }
    }
}
