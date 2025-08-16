package com.platform.coding.domain.review;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
// 학생 한 명은 하나에 대해 후기를 한 번만 작성할 수 있도록 유니크 제약조건을 설정함.
@Table(name = "reviews", uniqueConstraints = { @UniqueConstraint(name = "uk_student_course_review", columnNames = {"student_id", "course_id"}) }, schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 별점 기능 (1~5)
    @Check(constraints = "rating >= 1 AND rating <= 5")
    @Column(nullable = false)
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ReviewStatus status;

    // 베스트 후기 여부
    @Column(name = "is_best", nullable = false)
    private boolean isBest = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewReport> reports = new ArrayList<>();

    @Builder
    public Review(Course course, User student, String content, Integer rating) {
        // 방어 로직
        if (student.getUserType() != UserType.STUDENT) {
            throw new IllegalArgumentException("리뷰는 학생 계정으로만 작성할 수 있습니다.");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1점에서 5점 사이여야 합니다.");
        }

        this.course = course;
        this.student = student;
        this.content = content;
        this.rating = rating;
        // 초기 상태는 '승인 대기'
        this.status = ReviewStatus.PENDING_APPROVAL;
        this.createdAt = Instant.now();
    }
    
    // 비즈니스 로직
    public void publish() {
        this.status = ReviewStatus.PUBLISHED;
    }

    public void hide() {
        this.status = ReviewStatus.HIDDEN;
    }

    public void markAsBest(boolean isBest) {
        this.isBest = isBest;
    }

    public void report() {
        this.status = ReviewStatus.REPORTED;
    }
    
    // 관리자가 신고된 리뷰를 다시 게시 상태로 되돌리는 로직
    public void revert() {
        if (this.status != ReviewStatus.REPORTED) {
            throw new IllegalStateException("신고된 상태의 리뷰만 되돌릴 수 있습니다.");
        }
        this.status = ReviewStatus.PUBLISHED;
    }
}
