package com.platform.coding.controller.notification;

import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.notification.Notification;
import com.platform.coding.domain.notification.NotificationRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class NotificationControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private User user;
    private String userToken;
    private Notification notification1, notification2, otherUserNotification;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        user = userRepository.save(User.builder()
                .email("test@example.com")
                .passwordHash("password_hash123")
                .userName("테스트유저")
                .userType(UserType.PARENT)
                .build());
        User otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .passwordHash("password_hash456")
                .userName("다른유저")
                .userType(UserType.PARENT)
                .build());
        userToken = jwtUtil.createAccessToken(user);

        // 테스트 데이터 생성
        notification1 = notificationRepository.save(new Notification(user, "첫 번째 알림", "/link1"));
        notification2 = notificationRepository.save(new Notification(user, "두 번째 알림", "/link2"));
        otherUserNotification = notificationRepository.save(new Notification(otherUser, "다른 사용자의 알림", "/link3"));
    }

    @Test
    @DisplayName("로그인한 사용자는 자신의 알림 목록을 페이지네이션으로 조회할 수 있다.")
    void getNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + userToken)
                        .param("size", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                // 최신순 정렬 확인
                .andExpect(jsonPath("$.content[0].content").value("두 번째 알림"));
    }

    @Test
    @DisplayName("사용자는 자신의 특정 알림을 '읽음' 상태로 변경할 수 있다.")
    void markAsRead() throws Exception {
        // given: notification1은 아직 읽지 않은 상태 (isRead=false)
        assertThat(notification1.isRead()).isFalse();

        // when & then
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notification1.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + userToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        // DB에서 직접 확인
        Notification updatedNotification = notificationRepository.findById(notification1.getId()).orElseThrow();
        assertThat(updatedNotification.isRead()).isTrue();
    }

    @Test
    @DisplayName("다른 사용자의 알림을 '읽음' 처리하려고 하면 실패(400 Bad Request)해야 한다.")
    void markAsReadFailForOthersNotification() throws Exception {
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", otherUserNotification.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + userToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("자신의 알림만 읽음 처리할 수 있습니다."));
    }

    @Test
    @DisplayName("사용자는 자신의 모든 알림을 '읽음' 상태로 한번에 변경할 수 있다.")
    void markAllAsRead() throws Exception {
        // given: user에게는 2개의 읽지 않은 알림이 있음
        assertThat(notificationRepository.findAll().stream().filter(n -> n.getUser().getId().equals(user.getId()) && !n.isRead()).count()).isEqualTo(2);

        // when & then
        mockMvc.perform(post("/api/notifications/read-all")
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + userToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        // DB에서 직접 확인
        assertThat(notificationRepository.findAll().stream().filter(n -> n.getUser().getId().equals(user.getId()) && !n.isRead()).count()).isZero();
    }
}
