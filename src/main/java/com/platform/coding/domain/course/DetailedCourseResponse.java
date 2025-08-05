package com.platform.coding.domain.course;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

// 강의 상세 조회용 DTO
public record DetailedCourseResponse(
        Long courseId,
        String title,
        String description,
        String instructorName,
        BigDecimal price,
        List<ChapterDTO> curriculum
) {
    @Builder
    public DetailedCourseResponse {}

    // 내부 DTO: Capture 정보를 담음
    public record ChapterDTO(
            Long chapterId,
            String title,
            List<LectureDTO> lectures
    ) {
        @Builder
        public ChapterDTO {}
    }

    // 내부 DTO: Lecture 정보를 담음
    public record LectureDTO(
        Long lectureId,
        String title,
        boolean isSample
    ) {
        @Builder
        public LectureDTO {}
    }

    public static DetailedCourseResponse fromEntity(Course course) {
        List<ChapterDTO> chapterDTOS = course.getChapters().stream()
                .map (chapter -> ChapterDTO.builder()
                        .chapterId(chapter.getId())
                        .title(chapter.getTitle())
                        .lectures(chapter.getLectures().stream()
                                .map(lecture -> LectureDTO.builder()
                                        .lectureId(lecture.getId())
                                        .title(lecture.getTitle())
                                        .isSample(lecture.isSample())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return DetailedCourseResponse.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .instructorName(course.getAdmin().getUserName())
                .price(course.getPrice())
                .curriculum(chapterDTOS)
                .build();
    }
}
