package com.platform.coding.domain.course;

import com.platform.coding.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses", schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    // 강의를 생성한 관리자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;
    
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Setter
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.DRAFT;

    // NUMERIC(10, 2)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Course(1) : Chapter(N) 양방향 관계
    // 'mappedBy'는 연관관계의 주인이 아님을 명시합니다 (Chapter.course가 주인).
    // 'cascade = CascadeType.ALL'은 Course 저장/삭제 시 Chapter도 함께 처리됨을 의미합니다.
    // 'orphanRemoval = true'는 컬렉션에서 Chapter를 제거하면 DB에서도 삭제되도록 합니다.
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chapter> chapters = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public Course(User admin, String title, String description, BigDecimal price) {
        this.admin = admin;
        this.title = title;
        this.description = description;
        this.price = price;
    }

    // 연관관계 편의 메소드
    public void addChapter(Chapter chapter) {
        this.chapters.add(chapter);
        chapter.setCourse(this);
    }

    public void updateDetails(String title, String description, BigDecimal price) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if(price != null) {
            this.price = price;
        }
    }
    
    // 보관(논리적 삭제) 처리
    public void archive() {
        if (this.status == CourseStatus.ARCHIVED) {
            return;
        }
        this.status = CourseStatus.ARCHIVED;
    }
}
