package com.platform.coding.service.admin.dto;

import com.platform.coding.domain.submission.Submission;
import lombok.Builder;

import java.time.Instant;

/**
 * 관리자 과제 목록 조회용 요약 DTO
 */
public record SubmissionSummaryResponse(
        Long submissionId,
        String courseTitle,
        String assignmentTitle,
        String studentName,
        Instant submittedAt,
        String status
) {
    @Builder
    public SubmissionSummaryResponse {}

    public static SubmissionSummaryResponse fromEntity(Submission submission) {
        return SubmissionSummaryResponse.builder()
                .submissionId(submission.getId())
                .courseTitle(submission.getAssignment().getLecture().getChapter().getCourse().getTitle())
                .assignmentTitle(submission.getAssignment().getTitle())
                .studentName(submission.getStudent().getUserName())
                .submittedAt(submission.getSubmittedAt())
                .status(submission.getStatus().name())
                .build();
    }
}