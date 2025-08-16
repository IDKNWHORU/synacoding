package com.platform.coding.service.lecture.dto;

import com.platform.coding.domain.course.Lecture;
import lombok.Builder;

public record LectureViewResponse(
        Long lectureId,
        String title,
        String videoUrl,
        // 이어보기 기능의 핵심
        int lastViewedSeconds
) {
    @Builder
    public LectureViewResponse {}

    public static LectureViewResponse of(Lecture lecture, int lastViewedSeconds) {
        return new LectureViewResponse(lecture.getId(),
                lecture.getTitle(),
                lecture.getVideoUrl(),
                lastViewedSeconds);
    }
}
