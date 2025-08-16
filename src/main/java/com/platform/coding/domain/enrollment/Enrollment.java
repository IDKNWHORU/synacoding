package com.platform.coding.domain.enrollment;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}), schema = "platform")
@Getter
@NoArgsConstructor
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EnrollmentStatus status;

    @Column(name = "progress_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal progressRate = BigDecimal.ZERO;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder
    public Enrollment(User student, Course course) {
        // 수강 신청은 학생(STUDENT) 계정만 가능해야 합니다.
        if (student.getUserType() != UserType.STUDENT) {
            throw new IllegalArgumentException("수강 신청은 학생 계정으로만 가능합니다.");
        }

        this.student = student;
        this.course = course;
        this.status = EnrollmentStatus.IN_PROGRESS;
        this.enrolledAt = Instant.now();
    }

    /**
     * 전체 강의 진도율을 업데이트합니다.
     * @param newProgressRate 새로운 진도율 (0.00 ~ 100.00)
     */
    public void updateProgress(BigDecimal newProgressRate) {
        if (newProgressRate.compareTo(BigDecimal.ZERO) < 0 || newProgressRate.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException("진도율은 0과 100 사이의 값이어야 합니다.");
        }

        this.progressRate = newProgressRate;

        // 진도율이 100%가 되면 상태를 '완료'로 변경하고 완료 시간을 기록합니다.
        if (newProgressRate.compareTo(new BigDecimal("100.00")) >= 0) {
            this.status = EnrollmentStatus.COMPLETED;
            this.completedAt = Instant.now();
        }
    }

    /**
     * 환불을 요청하여 상태를 변경합니다.
     */
    public void requestRefund() {
        if (this.status != EnrollmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("수강 중인 강의만 환불 요청이 가능합니다. 현재 상태: " + this.status);
        }
        this.status = EnrollmentStatus.REFUND_REQUESTED;
    }
}
