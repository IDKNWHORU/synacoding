package com.platform.coding.service.lecture.dto;

import lombok.Builder;

/**
 * 웹 동영상 시청 페이지에 필요한 데이터를 담는 DTO
 */
public record LectureWebResponse(
        Long lectureId,
        String lectureTitle,
        String videoUrl,
        int lastViewedSeconds,
        Long courseId,
        String courseTitle,
        Long previousLectureId,
        Long nextLectureId
) {
    @Builder
    public LectureWebResponse {}
}
