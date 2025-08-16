package com.platform.coding.controller.submission;

import com.platform.coding.domain.submission.Assignment;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.submission.SubmissionService;
import com.platform.coding.service.submission.dto.SubmissionRequest;
import com.platform.coding.service.submission.dto.SubmissionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 과제 제출 및 조회 관련 웹 페이지 요청을 처리하는 컨트롤러
 */
@Controller
@RequiredArgsConstructor
public class SubmissionWebController {

    private final SubmissionService submissionService;

    /**
     * 과제 제출 폼 페이지를 반환합니다.
     */
    @GetMapping("/assignments/{assignmentId}/submit")
    public String submissionForm(@PathVariable Long assignmentId,
                                 @AuthenticationPrincipal User student,
                                 Model model) {
        // 학생의 수강 여부 등 권한을 확인하고 과제 정보를 가져옵니다.
        Assignment assignment = submissionService.getAssignmentForSubmission(assignmentId, student);
        // 기존 제출 내역이 있다면 내용을 폼에 채워줍니다.
        SubmissionRequest request = submissionService.findMySubmissionByAssignment(student, assignment)
                .map(submission -> new SubmissionRequest(submission.getTextContent()))
                .orElse(new SubmissionRequest(""));

        model.addAttribute("assignment", assignment);
        model.addAttribute("submissionRequest", request);

        if (assignment.getContent() != null) {
            model.addAttribute("formattedContent", assignment.getContent().replace("\n", "<br />"));
        } else {
            model.addAttribute("formattedContent", "");
        }

        return "submission/form";
    }

    /**
     * 과제 제출(생성 또는 수정)을 처리합니다.
     */
    @PostMapping("/assignments/{assignmentId}/submit")
    public String handleSubmit(@PathVariable Long assignmentId,
                               @Valid @ModelAttribute("submissionRequest") SubmissionRequest request,
                               @AuthenticationPrincipal User student,
                               RedirectAttributes redirectAttributes) {

        Long submissionId = submissionService.submitOrUpdateAssignment(assignmentId, request, student);
        redirectAttributes.addFlashAttribute("successMessage", "과제가 성공적으로 제출되었습니다.");

        // 제출 내역 확인 페이지로 리디렉션
        return "redirect:/submissions/" + submissionId;
    }

    /**
     * 제출된 과제와 피드백을 확인하는 상세 페이지를 반환합니다.
     */
    @GetMapping("/submissions/{submissionId}")
    public String submissionDetail(@PathVariable Long submissionId,
                                   @AuthenticationPrincipal User student,
                                   Model model) {
        SubmissionResponse submissionResponse = submissionService.getMySubmission(submissionId, student);
        model.addAttribute("submission", submissionResponse);

        model.addAttribute("formattedMyContent", submissionResponse.myContent().replace("\n", "<br />"));
        if (submissionResponse.feedback() != null && submissionResponse.feedback().content() != null) {
            model.addAttribute("formattedFeedbackContent", submissionResponse.feedback().content().replace("\n", "<br />"));
        }

        return "submission/detail";
    }
}