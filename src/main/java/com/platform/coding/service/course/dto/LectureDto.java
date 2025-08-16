package com.platform.coding.service.course.dto;

import com.platform.coding.domain.course.Lecture;
import com.platform.coding.domain.submission.Assignment;

import java.util.List;

public record LectureDto(
        Long lectureId,
        int order,
        String title,
        boolean isSample,
        List<AssignmentDto> assignments
) {
    public static LectureDto of(Lecture lecture, List<Assignment> assignments) {
        List<AssignmentDto> assignmentDtos = assignments.stream()
                .map(AssignmentDto::of)
                .toList();
        return new LectureDto(lecture.getId(), lecture.getOrder(), lecture.getTitle(), lecture.isSample(), assignmentDtos);
    }
}