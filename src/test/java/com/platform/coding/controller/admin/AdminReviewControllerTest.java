package com.platform.coding.controller.admin;

import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.review.Review;
import com.platform.coding.domain.review.ReviewRepository;
import com.platform.coding.domain.review.ReviewStatus;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.admin.AdminReviewService;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AdminReviewControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private AdminReviewService adminReviewService;

    private User admin;
    private String adminToken;
    private String studentToken;
    private Review pendingReview;
    private Review publishedReview;
    private Review reportedReview;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash123")
                .userName("관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());
        User student = userRepository.save(User.builder()
                .email("student@example.com")
                .passwordHash("password_hash456")
                .userName("학생")
                .userType(UserType.STUDENT)
                .build());

        adminToken = jwtUtil.createAccessToken(admin);
        studentToken = jwtUtil.createAccessToken(student);

        Course pendingCourse = courseRepository.save(Course.builder()
                .admin(admin)
                .title("강의")
                .price(BigDecimal.ZERO)
                .build());

        Course publishedCourse = courseRepository.save(Course.builder()
                .admin(admin)
                .title("강의")
                .price(BigDecimal.ZERO)
                .build());

        Course reportedCourse = courseRepository.save(Course.builder()
                .admin(admin)
                .title("강의3")
                .price(BigDecimal.ZERO)
                .build());

        pendingReview = reviewRepository.save(Review.builder()
                .course(pendingCourse)
                .student(student)
                .content("승인 대기 중인 리뷰입니다.")
                .rating(5)
                .build());

        publishedReview = reviewRepository.save(Review.builder()
                .course(publishedCourse)
                .student(student)
                .content("이미 게시된 리뷰입니다.")
                .rating(4)
                .build());
        // 상태 변경 후 저장
        publishedReview.publish();
        reviewRepository.save(publishedReview);

        // 신고된 리뷰 데이터 추가
        reportedReview = reviewRepository.save(Review.builder()
                // 임의의 course 사용
                .course(reportedCourse)
                .student(student)
                .content("신고된 리뷰입니다.")
                .rating(1)
                .build());
        // 상태 변경
        reportedReview.report();
        reviewRepository.save(reportedReview);
    }

    @Test
    @DisplayName("관리자는 '승인 대기' 상태의 리뷰 목록을 조회할 수 있다.")
    void getPendingReviews() throws Exception {
        mockMvc.perform(get("/api/admin/reviews")
                    .param("status", ReviewStatus.PENDING_APPROVAL.name())
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].reviewId").value(pendingReview.getId()))
                .andExpect(jsonPath("$.content[0].status").value(ReviewStatus.PENDING_APPROVAL.name()));
    }

    @Test
    @DisplayName("관리자는 '승인 대기' 상태의 리뷰를 '게시됨'으로 변경할 수 있다.")
    void approveReviewSuccess() throws Exception {
        mockMvc.perform(patch("/api/admin/reviews/{reviewId}/approve", pendingReview.getId())
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        Review approvedReview = reviewRepository.findById(pendingReview.getId()).orElseThrow();
        assertThat(approvedReview.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
    }

    @Test
    @DisplayName("관리자는 이미 게시된 리뷰를 숨김 처리할 수 있다.")
    void hideReviewSuccess() throws Exception {
        mockMvc.perform(patch("/api/admin/reviews/{reviewId}/hide", publishedReview.getId())
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        Review hiddenReview = reviewRepository.findById(publishedReview.getId()).orElseThrow();
        assertThat(hiddenReview.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
    }

    @Test
    @DisplayName("일반 사용자가 리뷰 관리 API를 호출하면 실패(403 Forbidden)해야 한다.")
    void reviewApiFailWithStudentRole() throws Exception {
        mockMvc.perform(get("/api/admin/reviews")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + studentToken))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("존재하지 않는 리뷰를 승인하려고 하면 실패(400 Bad Request)해야 한다.")
    void approveNonExistentReviewFail() throws Exception {
        long nonExistentId  = 9999L;
        mockMvc.perform(patch("/api/admin/reviews/{reviewId}/approve", nonExistentId )
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 리뷰입니다."));
    }

    @Test
    @DisplayName("관리자는 '신고됨' 상태의 리뷰를 다시 '게시됨' 상태로 되돌릴 수 있다.")
    void revertReviewSuccess() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/admin/reviews/{reviewId}/revert", reportedReview.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        Review revertedReview = reviewRepository.findById(reportedReview.getId()).orElseThrow();
        assertThat(revertedReview.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
    }

    @Test
    @DisplayName("관리자는 게시된 리뷰를 '베스트 리뷰'로 선정할 수 있다.")
    void markAsBestSuccess() throws Exception {
        // given: publishedReview는 isBest가 false인 상태
        assertThat(publishedReview.isBest()).isFalse();

        // when & then
        mockMvc.perform(patch("/api/admin/reviews/{reviewId}/mark-as-best", publishedReview.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        Review bestReview = reviewRepository.findById(publishedReview.getId()).orElseThrow();
        assertThat(bestReview.isBest()).isTrue();
    }

    @Test
    @DisplayName("관리자는 '베스트 리뷰' 선정을 취소할 수 있다.")
    void unmarkAsBestSuccess() throws Exception {
        // given: 먼저 베스트 리뷰로 선정
        adminReviewService.markAsBest(publishedReview.getId(), admin);
        Review reviewToUnmark = reviewRepository.findById(publishedReview.getId()).orElseThrow();
        assertThat(reviewToUnmark.isBest()).isTrue();

        // when & then
        mockMvc.perform(patch("/api/admin/reviews/{reviewId}/unmark-as-best", publishedReview.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        Review notBestAnymore = reviewRepository.findById(publishedReview.getId()).orElseThrow();
        assertThat(notBestAnymore.isBest()).isFalse();
    }

    @Test
    @DisplayName("게시되지 않은(승인 대기) 리뷰를 '베스트 리뷰'로 선정하려고 하면 실패(400 Bad Request)해야 한다.")
    void markAsBestFailForPendingReview() throws Exception {
        // given: pendingReview의 상태는 PENDING_APPROVAL
        assertThat(pendingReview.getStatus()).isEqualTo(ReviewStatus.PENDING_APPROVAL);

        // when & then
        mockMvc.perform(patch("/api/admin/reviews/{reviewId}/mark-as-best", pendingReview.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"))
                .andExpect(jsonPath("$.message").value("게시된 상태의 리뷰만 베스트로 선정할 수 있습니다."));
    }
}
