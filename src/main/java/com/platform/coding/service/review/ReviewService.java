package com.platform.coding.service.review;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.enrollment.EnrollmentStatus;
import com.platform.coding.domain.payment.Reward;
import com.platform.coding.domain.payment.RewardRepository;
import com.platform.coding.domain.payment.RewardType;
import com.platform.coding.domain.review.*;
import com.platform.coding.domain.rewardpolicy.PolicyKey;
import com.platform.coding.domain.rewardpolicy.RewardPolicyRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.filter.ProfanityFilterService;
import com.platform.coding.service.review.dto.ReviewRequest;
import com.platform.coding.service.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RewardRepository rewardRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final RewardPolicyRepository rewardPolicyRepository;
    private final ProfanityFilterService profanityFilterService;

    // 리뷰 작성 및 포인트 지급
    @Transactional
    public Long createReviewAndProvidePoints(Long courseId, ReviewRequest request, User student) {
        // 금지어 포함 여부 검사 로직 추가
        if (profanityFilterService.containsProfanity(request.content())) {
            throw new IllegalArgumentException("리뷰에 부적절한 단어가 포함되어 있습니다.");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));
        
        // 권한 검증1: 이 강의를 '완강'했는지 확인
        Enrollment enrollment = enrollmentRepository.findByStudentAndCourse(student, course)
                .orElseThrow(() -> new IllegalArgumentException("수강 이력이 없는 강의입니다."));

        if (enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            throw new IllegalArgumentException("강의를 완강한 후에만 리뷰를 작성할 수 있습니다.");
        }

        // 권한 검증2: 이미 리뷰를 작성했는지 확인 (DB의 Unique Constraint로도 방지됨)
        reviewRepository.findByStudentAndCourse(student, course).ifPresent(r -> {
            throw new IllegalArgumentException("이미 리뷰를 작성한 강의입니다.");
        });

        // 리뷰 생성 및 저장
        Review newReview = Review.builder()
                .course(course)
                .student(student)
                .content(request.content())
                .rating(request.rating())
                .build();
        Review savedReview = reviewRepository.save(newReview);

        // --- 포인트 지급 로직 수정 ---
        // 1. 최소 글자 수 정책 조회
        int minLength = rewardPolicyRepository.findByPolicyKey(PolicyKey.REVIEW_REWARD_MIN_LENGTH)
                .map(policy -> Integer.parseInt(policy.getPolicyValue()))
                // 정책이 없으면 기본값 50
                .orElse(50);

        // 2. 지급 포인트 정책 조회
        BigDecimal rewardAmount = rewardPolicyRepository.findByPolicyKey(PolicyKey.REVIEW_REWARD_POINT_AMOUNT)
                .map(policy -> new BigDecimal(policy.getPolicyValue()))
                // 정책이 없으면 0
                .orElse(BigDecimal.ZERO);

        // 조건 충족 시 포인트 지급
        if (request.content().length() >= minLength && rewardAmount.compareTo(BigDecimal.ZERO) > 0) {
            Reward pointReward = Reward.builder()
                    .user(student)
                    .rewardType(RewardType.POINT)
                    .amount(rewardAmount)
                    .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                    .build();
            rewardRepository.save(pointReward);
        }

        // TODO: 리뷰 작성 및 포인트 지급에 대한 알림 발송 (향후 구현)

        return savedReview.getId();
    }

    /**
     * 특정 리뷰를 신고한다.
     * @param reviewId 신고할 리뷰 ID
     * @param reporter 신고하는 사용자
     */
    @Transactional
    public void reportReview(Long reviewId, User reporter) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));
        
        // 자신의 리뷰는 신고할 수 없음
        if (review.getStudent().getId().equals(reporter.getId())) {
            throw new IllegalArgumentException("자신이 작성한 리뷰는 신고할 수 없습니다.");
        }

        // 이미 신고한 리뷰는 중복 신고 불가
        if (reviewReportRepository.existsByReviewAndReporter(review, reporter)) {
            throw new IllegalArgumentException("이미 신고한 리뷰입니다.");
        }

        // 신고 기록 저장
        reviewReportRepository.save(new ReviewReport(review, reporter));

        // 리뷰 상태를 '신고됨'으로 변경
        review.report();
        
        // TODO: 관리자에게 알림을 보내는 로직 추가 기능
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getPublishedReviewsForCourse(Long courseId) {
        return reviewRepository.findByCourseIdAndStatus(courseId, ReviewStatus.PUBLISHED)
                .stream()
                .map(ReviewResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
