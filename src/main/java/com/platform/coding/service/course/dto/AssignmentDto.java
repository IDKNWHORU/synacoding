package com.platform.coding.service.course.dto;

import com.platform.coding.domain.submission.Assignment;

public record AssignmentDto(
        Long assignmentId,
        String title
) {
    public static AssignmentDto of(Assignment assignment) {
        return new AssignmentDto(assignment.getId(), assignment.getTitle());
    }
}