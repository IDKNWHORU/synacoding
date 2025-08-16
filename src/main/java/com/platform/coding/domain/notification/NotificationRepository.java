package com.platform.coding.domain.notification;

import com.platform.coding.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /**
     * 특정 사용자의 모든 알림을 최신순으로 페이지네이션하여 조회한다.
     */
    Page<Notification> findAllByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * 특정 사용자의 읽지 않은 모든 알림을 찾아 '읽음' 상태로 변경한다.
     * @return 변경된 알림의 수
     */
    // DB와 영속성 컨텍스트의 동기화를 위해 clear 옵션 추가
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadByUser(@Param("user") User user);

    /**
     * 특정 사용자의 읽지 않은 알림 개수를 조회한다.
     */
    long countByUserAndIsReadFalse(User user);
}
