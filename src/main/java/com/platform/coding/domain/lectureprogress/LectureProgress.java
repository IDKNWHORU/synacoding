package com.platform.coding.domain.lectureprogress;

import com.platform.coding.domain.course.Lecture;
import com.platform.coding.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "lecture_progress", uniqueConstraints = { @UniqueConstraint(name = "uk_student_lecture", columnNames = {"student_id", "lecture_id"})}, schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    // 마지막으로 강의를 시청한 시간 (초)
    @Column(name = "last_viewed_seconds", nullable = false)
    private int lastViewedSeconds;

    // 이 강의를 다 봤는지 여부
    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LectureProgress(User student, Lecture lecture) {
        this.student = student;
        this.lecture = lecture;
        this.lastViewedSeconds = 0;
        this.updatedAt = Instant.now();
    }

    public void updateProgress(int newViewedSeconds) {
        // 비상식적인 값 방어
        if (newViewedSeconds < 0 || newViewedSeconds > this.lecture.getDurationSeconds()) {
            return;
        }

        this.lastViewedSeconds = newViewedSeconds;
        this.updatedAt = Instant.now();
        
        // 시청 시간의 95% 이상을 보면 완료 처리
        if (this.lecture.getDurationSeconds() > 0 &&
                (double) this.lastViewedSeconds / this.lecture.getDurationSeconds() >= 0.95) {
            this.isCompleted = true;
        }
    }
}
