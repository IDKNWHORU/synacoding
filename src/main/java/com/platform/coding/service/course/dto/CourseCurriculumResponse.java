package com.platform.coding.service.course.dto;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.submission.Assignment;

import java.util.List;
import java.util.Map;

public record CourseCurriculumResponse(
        Long courseId,
        String courseTitle,
        List<ChapterDto> chapters
) {
    public static CourseCurriculumResponse of(Course course, Map<Long, List<Assignment>> assignmentsByLecture) {
        List<ChapterDto> chapterDtos = course.getChapters().stream()
                .map(chapter -> ChapterDto.of(chapter, assignmentsByLecture))
                .toList();
        return new CourseCurriculumResponse(course.getId(), course.getTitle(), chapterDtos);
    }
}