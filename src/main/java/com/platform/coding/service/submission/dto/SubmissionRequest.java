package com.platform.coding.service.submission.dto;

import com.platform.coding.domain.submission.SubmissionRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public record SubmissionRequest(
        @NotBlank(message = "제출할 내용을 입력해주세요.")
        String content
) {
    @Builder
    public SubmissionRequest {}
}
