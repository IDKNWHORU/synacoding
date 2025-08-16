package com.platform.coding.controller.review;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.course.CourseService;
import com.platform.coding.service.review.ReviewService;
import com.platform.coding.service.review.dto.ReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/courses/{courseId}/reviews")
@RequiredArgsConstructor
public class ReviewWebController {

    private final ReviewService reviewService;
    private final CourseService courseService;
    private final CourseRepository courseRepository;

    @GetMapping("/new")
    public String reviewForm(@PathVariable Long courseId, Model model, @AuthenticationPrincipal User student) {
        if (!courseService.isEligibleToWriteReview(courseId, student)) {
            return "redirect:/courses/" + courseId + "?error=not_eligible_for_review";
        }

        Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));
        model.addAttribute("course", course);
        model.addAttribute("reviewRequest", new ReviewRequest("", 5));
        return "reviews/form";
    }

    @PostMapping
    public String submitReview(@PathVariable Long courseId,
                               @Valid @ModelAttribute("reviewRequest") ReviewRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal User student,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        if (!courseService.isEligibleToWriteReview(courseId, student)) {
            redirectAttributes.addFlashAttribute("errorMessage", "리뷰를 작성할 권한이 없습니다.");
            return "redirect:/courses/" + courseId;
        }

        if (bindingResult.hasErrors()) {
            Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));
            model.addAttribute("course", course);
            return "reviews/form";
        }

        try {
            reviewService.createReviewAndProvidePoints(courseId, request, student);
            redirectAttributes.addFlashAttribute("successMessage", "소중한 후기를 남겨주셔서 감사합니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/courses/" + courseId;
    }
}