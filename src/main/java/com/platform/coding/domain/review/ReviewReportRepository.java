package com.platform.coding.domain.review;

import com.platform.coding.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    /**
     * 특정 사용자가 특정 리뷰를 신고했는지 여부를 확인합니다. (중복 신고 방지용)
     */
    boolean existsByReviewAndReporter(Review review, User reporter);
}
