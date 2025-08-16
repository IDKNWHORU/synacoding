package com.platform.coding.domain.review;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReviewRepositoryTest extends IntegrationTestSupport {
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;

    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        
        User admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash123")
                .userName("관리자")
                .userType(UserType.CONTENT_MANAGER)
                .build());
        student = userRepository.save(User.builder()
                .email("student@example.com")
                .passwordHash("password_hash456")
                .userName("학생")
                .userType(UserType.STUDENT)
                .build());
        course = courseRepository.save(Course.builder()
                .admin(admin)
                .title("강의")
                .price(BigDecimal.ZERO)
                .build());
    }

    @Test
    @DisplayName("학생이 강의에 대한 리뷰를 작성하면, 초기 상태는 PENDING_APPROVAL 이어야 한다.")
    void createReview() {
        // given
        String content = "정말 유익한 강의였습니다!";
        int rating = 5;

        // when
        Review review = Review.builder()
                .student(student)
                .course(course)
                .content(content)
                .rating(rating)
                .build();
        reviewRepository.save(review);

        // then
        Review foundReview = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(foundReview.getId()).isNotNull();
        assertThat(foundReview.getContent()).isEqualTo(content);
        assertThat(foundReview.getRating()).isEqualTo(rating);
        assertThat(foundReview.getStatus()).isEqualTo(ReviewStatus.PENDING_APPROVAL);
        assertThat(foundReview.getStudent().getId()).isEqualTo(student.getId());
        assertThat(foundReview.getCourse().getId()).isEqualTo(course.getId());
    }

    @Test
    @DisplayName("평점이 1~5 범위를 벗어나면 IllegalArgumentException이 발생해야 한다.")
    void createReviewFailWithInvalidRating() {
        // when & then
        assertThatThrownBy(() -> Review.builder()
                    .student(student)
                    .course(course)
                    .content("평점 오류 테스트")
                    // 유효하지 않은 평점
                    .rating(6)
                    .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("평점은 1점에서 5점 사이여야 합니다.");
    }

    @Test
    @DisplayName("한 학생이 같은 강의에 대해 리뷰를 중복 작성하면 DataIntegrityViolationException이 발생해야 한다.")
    void createReviewFailWithDuplicate() {
        // given: 첫 번째 리뷰를 미리 작성
        Review firstReview = Review.builder()
                .student(student)
                .course(course)
                .content("첫 리뷰")
                .rating(4)
                .build();
        reviewRepository.save(firstReview);
        
        // when & then: 동일한 학생과 강의로 두 번째 리뷰를 작성하려고 시도
        Review secondReview = Review.builder()
                .student(student)
                .course(course)
                .content("두 번째 리뷰")
                .rating(5)
                .build();

        // uk_student_course_review 유니크 제약조건 위반으로 예외 발생
        assertThatThrownBy(() -> reviewRepository.saveAndFlush(secondReview))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("리뷰의 상태를 PUBLISHED로 변경할 수 있어야 한다.")
    void updateReviewStatusToPublished() {
        // given
        Review review = reviewRepository.save(Review.builder()
                .student(student)
                .course(course)
                .content("...")
                .rating(5)
                .build());
        
        // when
        review.publish();
        // 변경사항을 DB에 즉시 반영
        reviewRepository.flush();

        // then
        Review foundReview = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(foundReview.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
    }
}
