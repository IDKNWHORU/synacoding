package com.platform.coding.domain.submission;

import com.platform.coding.domain.course.Lecture;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "assignments", schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    private Instant deadline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder
    public Assignment(Lecture lecture, String title, String content, Instant deadline) {
        this.lecture = lecture;
        this.title = title;
        this.content = content;
        this.deadline = deadline;
    }

    /**
     * 과제 정보 업데이트
     */
    public void updateDetails(String title, String content, Instant deadline) {
        this.title = title;
        this.content = content;
        this.deadline = deadline;
    }
}
