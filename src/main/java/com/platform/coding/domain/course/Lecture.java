package com.platform.coding.domain.course;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "lectures", schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lecture_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false)
    private String title;

    @Column(name = "video_url", length = 255)
    private String videoUrl;

    @Column(name = "lecture_order", nullable = false)
    private int order;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "is_sample", nullable = false)
    private boolean isSample = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder
    public Lecture(Chapter chapter, String title, String videoUrl, int order, Integer durationSeconds, boolean isSample) {
        this.chapter = chapter;
        this.title = title;
        this.videoUrl = videoUrl;
        this.order = order;
        this.durationSeconds = durationSeconds;
        this.isSample = isSample;
    }

    protected void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public void updateDetails(String title, int order, String videoUrl, boolean isSample, Integer durationSeconds) {
        this.title = title;
        this.order = order;
        this.videoUrl = videoUrl;
        this.isSample = isSample;
        this.durationSeconds = durationSeconds;
    }
}
