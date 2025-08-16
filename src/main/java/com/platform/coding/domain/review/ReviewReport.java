package com.platform.coding.domain.review;

import com.platform.coding.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "review_reports", uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "reporter_id"}), schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    // 신고한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(name = "reported_at", nullable = false, updatable = false)
    private Instant reportedAt;

    @Builder
    public ReviewReport(Review review, User reporter) {
        this.review = review;
        this.reporter = reporter;
        this.reportedAt = Instant.now();
    }
}