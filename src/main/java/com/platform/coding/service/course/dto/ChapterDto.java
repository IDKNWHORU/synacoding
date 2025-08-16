package com.platform.coding.service.course.dto;

import com.platform.coding.domain.course.Chapter;
import com.platform.coding.domain.submission.Assignment;

import java.util.List;
import java.util.Map;

public record ChapterDto(
        Long chapterId,
        int order,
        String title,
        List<LectureDto> lectures
) {
    public static ChapterDto of(Chapter chapter, Map<Long, List<Assignment>> assignmentsByLecture) {
        List<LectureDto> lectureDtos = chapter.getLectures().stream()
                .map(lecture -> LectureDto.of(lecture, assignmentsByLecture.getOrDefault(lecture.getId(), List.of())))
                .toList();
        return new ChapterDto(chapter.getId(), chapter.getOrder(), chapter.getTitle(), lectureDtos);
    }
}
