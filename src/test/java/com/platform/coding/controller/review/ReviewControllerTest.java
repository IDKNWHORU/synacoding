package com.platform.coding.controller.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.payment.RewardRepository;
import com.platform.coding.domain.review.Review;
import com.platform.coding.domain.review.ReviewReportRepository;
import com.platform.coding.domain.review.ReviewRepository;
import com.platform.coding.domain.review.ReviewStatus;
import com.platform.coding.domain.rewardpolicy.PolicyKey;
import com.platform.coding.domain.rewardpolicy.RewardPolicy;
import com.platform.coding.domain.rewardpolicy.RewardPolicyRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.review.ReviewService;
import com.platform.coding.service.review.dto.ReviewRequest;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class ReviewControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private RewardRepository rewardRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ReviewReportRepository reviewReportRepository;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private RewardPolicyRepository rewardPolicyRepository;

    private User student;
    private String studentToken;
    private Course completedCourse;
    private Course inProgressCourse;

    @BeforeEach
    void setUp() throws Exception {
        reviewReportRepository.deleteAllInBatch();
        reviewRepository.deleteAllInBatch();
        rewardRepository.deleteAllInBatch();
        enrollmentRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        User admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash123")
                .userName("관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());
        student = userRepository.save(User.builder()
                .email("student@example.com")
                .passwordHash("password_hash456")
                .userName("학생")
                .userType(UserType.STUDENT)
                .build());
        studentToken = jwtUtil.createAccessToken(student);

        completedCourse = courseRepository.save(Course.builder()
                .admin(admin)
                .title("완강한 강의")
                .price(BigDecimal.ZERO)
                .build());
        inProgressCourse = courseRepository.save(Course.builder()
                .admin(admin)
                .title("수강중인 강의")
                .price(BigDecimal.ZERO)
                .build());

        // '완강한 강의'에 대한 수강 정보 생성 및 상태 변경
        Enrollment completedEnrollment = enrollmentRepository.save(new Enrollment(student, completedCourse));
        // 완강 처리
        completedEnrollment.updateProgress(new BigDecimal("100.00"));

        // '수강중인 강의'에 대한 수강 정보 생성
        enrollmentRepository.save(new Enrollment(student, inProgressCourse));

        RewardPolicy minLengthPolicy = createPolicy(PolicyKey.REVIEW_REWARD_MIN_LENGTH, "10", "리뷰 최소 글자 수");
        RewardPolicy pointAmountPolicy = createPolicy(PolicyKey.REVIEW_REWARD_POINT_AMOUNT, "1000", "지급 포인트");
        rewardPolicyRepository.saveAll(List.of(minLengthPolicy, pointAmountPolicy));
    }

    // RewardPolicy 엔티티를 생성하는 헬퍼 메소드
    private RewardPolicy createPolicy(PolicyKey key, String value, String description) throws Exception {
        Constructor<RewardPolicy> constructor = RewardPolicy.class.getDeclaredConstructor();        constructor.setAccessible(true); // 접근 제한을 해제
        constructor.setAccessible(true); // 접근 제한을 해제
        RewardPolicy policy = constructor.newInstance(); // 인스턴스 생성

        // 리플렉션을 사용해 private 필드에 값 설정
        setField(policy, "policyKey", key);
        setField(policy, "policyValue", value);
        setField(policy, "description", description);
        return policy;
    }

    // Reflection을 사용하여 필드 값을 설정하는 헬퍼 메소드
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("강의를 완강한 학생이 리뷰를 작성하면 성공하고, 포인트가 지급되어야 한다.")
    void createReviewSuccessForCompletedCourse() throws Exception {
        // given
        ReviewRequest request = new ReviewRequest("정말 최고의 강의입니다!", 5);
        long initialRewardCount = rewardRepository.count();

        // when
        mockMvc.perform(post("/api/reviews/courses/{courseId}", completedCourse.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        // then
        // 리뷰가 1개 생성되었는지 확인
        assertThat(reviewRepository.count()).isEqualTo(1);
        // 포인트가 1개 지급되었는지 확인
        assertThat(rewardRepository.count()).isEqualTo(initialRewardCount + 1);
        assertThat(rewardRepository.findAll().get(0).getUser().getId()).isEqualTo(student.getId());
        assertThat(rewardRepository.findAll().get(0).getAmount()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("강의를 완강하지 않은 학생이 리뷰를 작성하면 실패(400 Bad Request)해야 한다.")
    void createReviewFailForInProgressCourse() throws Exception {
        // given
        ReviewRequest request = new ReviewRequest("아직 다 안들었지만 리뷰 남깁니다.", 4);

        // when & then
        mockMvc.perform(post("/api/reviews/courses/{courseId}", inProgressCourse.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("금지어가 포함된 리뷰를 작성하려고 하면 실패(400 Bad Request)해야 한다.")
    void createReviewFailWithProfanity() throws Exception {
        // given
        ReviewRequest request = new ReviewRequest("이런 바보 같은 강의는 처음 봅니다.", 1);

        // when & then
        mockMvc.perform(post("/api/reviews/courses/{courseId}", completedCourse.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("리뷰에 부적절한 단어가 포함되어 있습니다."));

        // DB에 리뷰가 저장되지 않았는지 확인
        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    @DisplayName("다른 학생은 게시된 리뷰를 성공적으로 신고할 수 있다.")
    void reportReviewSuccess() throws Exception {
        // given: 신고할 대상 리뷰(publishedReview)와 신고자(otherStudent)를 이 테스트 내에서 생성
        Review publishedReview = reviewRepository.save(Review.builder()
                .course(completedCourse)
                .student(student)
                .content("이것은 게시된 리뷰입니다.")
                .rating(5)
                .build());
        publishedReview.publish();
        reviewRepository.save(publishedReview);

        User otherStudent = userRepository.save(User.builder()
                .email("other.student@example.com")
                .passwordHash("password_hash789")
                .userName("다른학생")
                .userType(UserType.STUDENT)
                .build());
        String otherStudentToken = jwtUtil.createAccessToken(otherStudent);

        // when & then
        mockMvc.perform(post("/api/reviews/{reviewId}/report", publishedReview.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + otherStudentToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        // DB 검증
        Review reportedReview = reviewRepository.findById(publishedReview.getId()).orElseThrow();
        assertThat(reportedReview.getStatus()).isEqualTo(ReviewStatus.REPORTED);
    }

    @Test
    @DisplayName("리뷰를 작성한 본인은 자신의 리뷰를 신고할 수 없다.")
    void reportReviewFailForOwnReview() throws Exception {
        // given: 신고할 대상 리뷰 생성
        Review publishedReview = reviewRepository.save(Review.builder()
                .course(completedCourse)
                .student(student)
                .content("이것은 게시된 리뷰입니다.")
                .rating(5)
                .build());
        publishedReview.publish();
        reviewRepository.save(publishedReview);

        // when & then
        mockMvc.perform(post("/api/reviews/{reviewId}/report", publishedReview.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + studentToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("자신이 작성한 리뷰는 신고할 수 없습니다."));
    }

    @Test
    @DisplayName("이미 신고한 리뷰를 중복으로 신고할 수 없다.")
    void reportReviewFailForDuplicateReport() throws Exception {
        // given: 신고할 대상 리뷰(publishedReview)와 신고자(otherStudent)를 이 테스트 내에서 생성
        Review publishedReview = reviewRepository.save(Review.builder()
                .course(completedCourse)
                .student(student)
                .content("이것은 게시된 리뷰입니다.")
                .rating(5)
                .build());
        publishedReview.publish();
        reviewRepository.save(publishedReview);

        User otherStudent = userRepository.save(User.builder()
                .email("other.student@example.com")
                .passwordHash("password_hash789")
                .userName("다른학생")
                .userType(UserType.STUDENT)
                .build());
        String otherStudentToken = jwtUtil.createAccessToken(otherStudent);

        // otherStudent가 미리 한번 신고함
        reviewService.reportReview(publishedReview.getId(), otherStudent);

        // when & then: otherStudent가 다시 신고 시도
        mockMvc.perform(post("/api/reviews/{reviewId}/report", publishedReview.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + otherStudentToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 신고한 리뷰입니다."));
    }
}
