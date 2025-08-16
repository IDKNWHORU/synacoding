package com.platform.coding.controller.admin;

import com.platform.coding.domain.submission.SubmissionStatus;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.admin.AdminCourseService;
import com.platform.coding.service.admin.dto.FeedbackRequest;
import com.platform.coding.service.admin.dto.SubmissionSummaryResponse;
import com.platform.coding.service.submission.dto.SubmissionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자용 과제 평가 및 피드백 관련 웹 요청을 처리하는 컨트롤러
 */
@Controller
@RequestMapping("/admin/submissions")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_MANAGER')")
@RequiredArgsConstructor
public class AdminSubmissionWebController {

    private final AdminCourseService adminCourseService;

    /**
     * 제출된 과제 목록 페이지
     * @param status 필터링할 제출 상태 (기본값: SUBMITTED)
     */
    @GetMapping
    public String listSubmissions(@RequestParam(required = false, defaultValue = "SUBMITTED") SubmissionStatus status,
                                  @PageableDefault(sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable,
                                  Model model) {

        Page<SubmissionSummaryResponse> submissionPage = adminCourseService.getSubmissionsByStatus(status, pageable);
        model.addAttribute("submissionPage", submissionPage);
        model.addAttribute("currentStatus", status.name());
        // 페이징 처리를 위한 정보 추가
        int nowPage = submissionPage.getPageable().getPageNumber() + 1;
        int startPage = Math.max(nowPage - 4, 1);
        int endPage = Math.min(nowPage + 4, submissionPage.getTotalPages());
        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage == 0 ? 1 : endPage);

        return "admin/submission_list";
    }

    /**
     * 과제 상세 및 피드백 작성 폼 페이지
     */
    @GetMapping("/{submissionId}")
    public String feedbackForm(@PathVariable Long submissionId, Model model, @AuthenticationPrincipal User admin) {
        SubmissionResponse submission = adminCourseService.getSubmissionForFeedback(submissionId, admin);
        model.addAttribute("submission", submission);
        // 아직 피드백이 없는 경우에만 폼 객체를 전달
        if (submission.feedback() == null) {
            model.addAttribute("feedbackRequest", new FeedbackRequest(""));
        }
        return "admin/submission_feedback_form";
    }

    /**
     * 피드백 제출 처리
     */
    @PostMapping("/{submissionId}")
    public String submitFeedback(@PathVariable Long submissionId,
                                 @Valid @ModelAttribute("feedbackRequest") FeedbackRequest request,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal User admin,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // 유효성 검증 실패 시, 폼 페이지를 다시 보여줌
            SubmissionResponse submission = adminCourseService.getSubmissionForFeedback(submissionId, admin);
            model.addAttribute("submission", submission);
            return "admin/submission_feedback_form";
        }

        adminCourseService.createFeedbackForSubmission(submissionId, request, admin);
        redirectAttributes.addFlashAttribute("successMessage", "피드백이 성공적으로 등록되었습니다.");
        return "redirect:/admin/submissions";
    }
}