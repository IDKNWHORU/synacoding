package com.platform.coding.service.admin;

import com.platform.coding.domain.review.Review;
import com.platform.coding.domain.review.ReviewRepository;
import com.platform.coding.domain.review.ReviewStatus;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.admin.dto.ReviewManagementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReviewService {
    private final ReviewRepository reviewRepository;

    /**
     * 특정 상태의 모든 리뷰를 페이지네이션하여 조회합니다.
     * @param status 조회할 리뷰 상태 (null일 경우 모든 상태 조회)
     * @param pageable 페이지 정보
     * @return 리뷰 목록
     */
    @Transactional(readOnly = true)
    public Page<ReviewManagementResponse> getReviewsByStatus(ReviewStatus status, Pageable pageable) {
        if (status == null) {
            return reviewRepository.findAll(pageable).map(ReviewManagementResponse::fromEntity);
        }
        return reviewRepository.findByStatus(status, pageable).map(ReviewManagementResponse::fromEntity);
    }

    /**
     * 리뷰를 승인(게시)합니다.
     * @param reviewId 승인할 리뷰 ID
     * @param admin 관리자 정보
     */
    @Transactional
    public void approveReview(Long reviewId, User admin) {
        Review review = findReviewById(reviewId);
        if (review.getStatus() != ReviewStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("승인 대기 중인 리뷰만 승인할 수 있습니다.");
        }
        review.publish();
    }

    /**
     * 리뷰를 숨김 처리합니다.
     * @param reviewId 숨길 리뷰 ID
     * @param admin 관리자 정보
     */
    @Transactional
    public void hideReview(Long reviewId, User admin) {
        Review review = findReviewById(reviewId);
        review.hide();
    }

    /**
     * 신고된 리뷰를 다시 게시 상태로 되돌린다.
     * @param reviewId 되돌릴 리뷰 ID
     * @param admin 관리자 정보
     */
    @Transactional
    public void reverReview(Long reviewId, User admin) {
        Review review = findReviewById(reviewId);
        review.revert();
    }

    /**
     * 리뷰를 '베스트 리뷰'로 선정합니다.
     * @param reviewId 베스트로 선정할 리뷰 ID
     * @param admin 관리자 정보
     */
    @Transactional
    public void markAsBest(Long reviewId, User admin) {
        Review review = findReviewById(reviewId);
        // 게시된 상태의 리뷰만 베스트로 선정할 수 있습니다.
        if (review.getStatus() != ReviewStatus.PUBLISHED) {
            throw new IllegalStateException("게시된 상태의 리뷰만 베스트로 선정할 수 있습니다.");
        }
        review.markAsBest(true);
    }

    /**
     * '베스트 리뷰' 선정을 취소합니다.
     * @param reviewId 취소할 리뷰 ID
     * @param admin 관리자 정보
     */
    @Transactional
    public void unmarkAsBest(Long reviewId, User admin) {
        Review review = findReviewById(reviewId);
        review.markAsBest(false);
    }

    private Review findReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));
    }
}
