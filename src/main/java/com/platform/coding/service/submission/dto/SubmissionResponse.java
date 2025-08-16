package com.platform.coding.service.submission.dto;

import com.platform.coding.domain.submission.Feedback;
import com.platform.coding.domain.submission.Submission;
import lombok.Builder;

import java.time.Instant;

public record SubmissionResponse(
        Long submissionId,
        Long assignmentId,
        String assignmentTitle,
        Long courseId,
        String courseTitle,
        String studentName,
        // 내가 제출한 내용
        String myContent,
        String status,
        Instant submittedAt,
        FeedbackDto feedback
) {
    @Builder
    public SubmissionResponse {}

    // 피드백 정보를 담을 내부 DTO
    public record FeedbackDto(
            Long feedbackId,
            String adminName,
            String content,
            Instant createdAt
    ) {
        @Builder
        public FeedbackDto {}
    }

    public static SubmissionResponse fromEntity(Submission submission) {
        Feedback feedback = submission.getFeedback();
        FeedbackDto feedbackDto = (feedback != null) ?
                FeedbackDto.builder()
                        .feedbackId(feedback.getId())
                        .adminName(feedback.getAdmin().getUserName())
                        .content(feedback.getContent())
                        .createdAt(feedback.getCreatedAt())
                        .build()
                :null;

        return SubmissionResponse.builder()
                .submissionId(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .assignmentTitle(submission.getAssignment().getTitle())
                .courseId(submission.getAssignment().getLecture().getChapter().getCourse().getId())
                .courseTitle(submission.getAssignment().getLecture().getChapter().getCourse().getTitle())
                .studentName(submission.getStudent().getUserName())
                .myContent(submission.getTextContent())
                .status(submission.getStatus().name())
                .submittedAt(submission.getSubmittedAt())
                .feedback(feedbackDto)
                .build();
    }
}
