package com.platform.coding.controller.submission;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.submission.SubmissionService;
import com.platform.coding.service.submission.dto.SubmissionRequest;
import com.platform.coding.service.submission.dto.SubmissionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;

    //  과제 제출 API
    @PostMapping("/assignments/{assignmentId}/submissions")
    public ResponseEntity<Void> submitAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody SubmissionRequest request,
            @AuthenticationPrincipal User student
            ) {
        Long submissionId = submissionService.submitAssignment(assignmentId, request, student);

        return ResponseEntity.created(URI.create("/api/submissions" + submissionId)).build();
    }

    // 내 제출물 조회
    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<SubmissionResponse> getSubmission(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal User student
    ) {
        SubmissionResponse response = submissionService.getMySubmission(submissionId, student);
        return ResponseEntity.ok(response);
    }
}
