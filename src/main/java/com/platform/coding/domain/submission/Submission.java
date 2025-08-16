package com.platform.coding.domain.submission;

import com.platform.coding.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "submissions", uniqueConstraints = { @UniqueConstraint(name = "uk_student_assignment", columnNames = {"student_id", "assignment_id"}) }, schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Lob
    @Column(name = "text_content", columnDefinition = "TEXT")
    // 학생이 제출한 내용
    private String textContent;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;
    
    // Submission(1) : Feedback(1) 양방향 관계
    @OneToOne(mappedBy = "submission", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Feedback feedback;

    @Builder
    public Submission(Assignment assignment, User student, String textContent) {
        this.assignment = assignment;
        this.student = student;
        this.textContent = textContent;
        // 제출 시 상태는 'SUBMITTED'
        this.status = SubmissionStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }
    
    // 연관관계 편의 메소드
    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
        // 피드백이 달리면 상태를 '채점 완료'로 변경
        this.status = SubmissionStatus.GRADED;
    }

    // 재제출 시 내용을 업데이트하는 메소드
    public void updateContent(String newContent) {
        this.textContent = newContent;
        this.status = SubmissionStatus.SUBMITTED;
        this.feedback = null;
    }
}
