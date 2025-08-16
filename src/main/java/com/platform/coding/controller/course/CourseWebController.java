package com.platform.coding.controller.course;

import com.platform.coding.domain.course.DetailedCourseResponse;
import com.platform.coding.domain.course.SimpleCourseResponse;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.course.CourseService;
import com.platform.coding.service.review.ReviewService;
import com.platform.coding.service.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseWebController {
    private final CourseService courseService;
    private final ReviewService reviewService;
    
    // 강의 목록 페이지
    @GetMapping
    public String getAllCoursesPage(
            @PageableDefault(size = 9, sort = "createdAt")Pageable pageable,
            Model model
            ) {
        Page<SimpleCourseResponse> coursesPage = courseService.getAllPublishedCourses(pageable);
        model.addAttribute("coursesPage", coursesPage);

        // 페이징 블록 계산
        int nowPage = coursesPage.getPageable().getPageNumber() + 1;
        int startPage = Math.max(nowPage - 4, 1);
        int endPage = Math.min(nowPage + 4, coursesPage.getTotalPages());
        if (coursesPage.getTotalPages() == 0) {
            endPage = 1;
        }

        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "courses/list";
    }

    /**
     * 강의 상세 페이지
     * @param courseId  URL 경로에서 가져온 강의 ID
     * @param model     뷰에 데이터를 전달할 Model 객체
     * @return          강의 상세 페이지 뷰 템플릿 경로
     */
    @GetMapping("/{courseId}")
    public String getCourseDetailPage(@PathVariable Long courseId, Model model, @AuthenticationPrincipal User student) {
        // 강의 상세 정보 조회
        DetailedCourseResponse courseDetails = courseService.getCourseDetails(courseId);
        model.addAttribute("course", courseDetails);
        
        // 게시된 리뷰 목록 조회
        List<ReviewResponse> reviews = reviewService.getPublishedReviewsForCourse(courseId);
        model.addAttribute("reviews", reviews);
        
        // 현재 사용자가 리뷰를 작성할 수 있는지 확인
        boolean canWriteReview = courseService.isEligibleToWriteReview(courseId, student);
        model.addAttribute("canWriteReview", canWriteReview);

        return "courses/detail";
    }
}
