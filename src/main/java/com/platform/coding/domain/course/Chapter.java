package com.platform.coding.domain.course;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chapters", schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chapter_id")
    private Long id;

    // 연관관계의 주인 (외래 키를 관리)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(name = "chapter_order", nullable = false)
    private int order;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lecture> lectures = new ArrayList<>();

    @Builder
    public Chapter(Course course, String title, int order) {
        this.course = course;
        this.title = title;
        this.order = order;
    }

    // 연관관계 편의 메소드
    public void addLecture(Lecture lecture) {
        this.lectures.add(lecture);
        lecture.setChapter(this);
    }
    
    // Course 엔티티에서 연관관계를 설정하기 위한 Setter
    protected void setCourse(Course course) {
        this.course = course;
    }

    public void updateDetails(String title, int order) {
        this.title = title;
        this.order = order;
    }
}
