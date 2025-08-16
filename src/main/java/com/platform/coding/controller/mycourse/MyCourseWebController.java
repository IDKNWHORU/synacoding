package com.platform.coding.controller.mycourse;

import com.platform.coding.domain.course.DetailedCourseResponse;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.course.CourseService;
import com.platform.coding.service.mycourse.MyCourseService;
import com.platform.coding.service.mycourse.dto.MyCourseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/my-courses")
@RequiredArgsConstructor
public class MyCourseWebController {
    private final MyCourseService myCourseService;
    private final CourseService courseService;

    @GetMapping
    public String myCoursesPage(@AuthenticationPrincipal User student, Model model) {
        List<MyCourseResponse> myCourses = myCourseService.getMyCourses(student);
        model.addAttribute("myCourses", myCourses);

        return "my-courses/list";
    }

    /**
     * 수강 중인 특정 강의의 커리큘럼 페이지를 보여줍니다.
     */
    @GetMapping("/{courseId}")
    public String courseCurriculumPage(@PathVariable Long courseId, @AuthenticationPrincipal User student, Model model) {
        // 수강 중인 강의가 맞는지 권한 검사를 포함하여 강의 상세 정보를 조회합니다.
        DetailedCourseResponse courseDetails = courseService.getEnrolledCourseDetails(courseId, student);
        model.addAttribute("course", courseDetails);
        return "my-courses/curriculum";
    }
}
