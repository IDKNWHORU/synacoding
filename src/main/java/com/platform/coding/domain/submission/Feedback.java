package com.platform.coding.domain.submission;

import com.platform.coding.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "feedbacks", schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long id;
    
    // 연관관계의 주인 (외래 키를 관리)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private Submission submission;

    // 피드백을 작성한 관리자(강사)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    // 피드백한 내용
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public Feedback(Submission submission, User admin, String content) {
        this.submission = submission;
        this.admin = admin;
        this.content = content;
        this.createdAt = Instant.now();
    }
}
