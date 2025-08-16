package com.platform.coding.controller.admin;

import com.platform.coding.domain.course.*;
import com.platform.coding.domain.submission.Assignment;
import com.platform.coding.domain.submission.AssignmentRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.admin.AdminCourseService;
import com.platform.coding.service.admin.dto.*;
import com.platform.coding.service.course.dto.CourseCurriculumResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 관리자 페이지의 강의/커리큘럼 관련 웹 요청을 처리하는 MVC 컨트롤러
 */
@Controller
@RequestMapping("/admin/courses")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_MANAGER')")
@RequiredArgsConstructor
public class AdminCourseWebController {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;
    private final AdminCourseService adminCourseService;
    private final AssignmentRepository assignmentRepository;

    /**
     * 강의 관리 메인 페이지 (강의 목록)
     */
    @GetMapping
    public String listCourses(Model model) {
        List<Course> courses = courseRepository.findAll();
        model.addAttribute("courses", courses);
        return "admin/course_management";
    }

    /**
     * 신규 강의 등록 폼 페이지
     */
    @GetMapping("/new")
    public String newCourseForm(Model model) {
        model.addAttribute("courseRequest", new CourseCreateRequest("", "", null));
        model.addAttribute("formAction", "/admin/courses");
        return "admin/course_form";
    }

    /**
     * 신규 강의 생성 처리
     */
    @PostMapping
    public String createCourse(@Valid @ModelAttribute("courseRequest") CourseCreateRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal User admin,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/course_form";
        }
        adminCourseService.createCourse(request, admin);
        redirectAttributes.addFlashAttribute("successMessage", "신규 강의가 성공적으로 등록되었습니다.");
        return "redirect:/admin/courses";
    }

    /**
     * 강의 수정 폼 페이지
     */
    @GetMapping("/{courseId}/edit")
    public String editCourseForm(@PathVariable Long courseId, Model model) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .build();

        model.addAttribute("courseRequest", request);
        model.addAttribute("courseId", courseId);
        model.addAttribute("formAction", "/admin/courses/" + courseId + "/edit");
        return "admin/course_form";
    }

    /**
     * 강의 수정 처리
     */
    @PostMapping("/{courseId}/edit")
    public String updateCourse(@PathVariable Long courseId,
                               @Valid @ModelAttribute("courseRequest") CourseUpdateRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal User admin,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/course_form";
        }
        adminCourseService.updateCourse(courseId, request, admin);
        redirectAttributes.addFlashAttribute("successMessage", "강의 정보가 성공적으로 수정되었습니다.");
        return "redirect:/admin/courses";
    }

    /**
     * 강의 게시 처리
     */
    @PostMapping("/{courseId}/publish")
    public String publishCourse(@PathVariable Long courseId, @AuthenticationPrincipal User admin, RedirectAttributes redirectAttributes) {
        adminCourseService.publishCourse(courseId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "강의가 성공적으로 게시되었습니다.");
        return "redirect:/admin/courses";
    }

    /**
     * 강의 게시 취소 처리
     */
    @PostMapping("/{courseId}/unpublish")
    public String unpublishCourse(@PathVariable Long courseId, @AuthenticationPrincipal User admin, RedirectAttributes redirectAttributes) {
        adminCourseService.unpublishCourse(courseId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "강의 게시가 취소되어 초안 상태로 변경되었습니다.");
        return "redirect:/admin/courses";
    }

    /**
     * 보관된 강의 재게시 처리
     */
    @PostMapping("/{courseId}/republish")
    public String republishCourse(@PathVariable Long courseId, @AuthenticationPrincipal User admin, RedirectAttributes redirectAttributes) {
        adminCourseService.republishCourse(courseId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "강의가 다시 게시되었습니다.");
        return "redirect:/admin/courses";
    }

    /**
     * 강의 보관(논리적 삭제) 처리
     */
    @PostMapping("/{courseId}/archive")
    public String archiveCourse(@PathVariable Long courseId, @AuthenticationPrincipal User admin, RedirectAttributes redirectAttributes) {
        adminCourseService.archiveCourse(courseId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "강의가 보관 처리되었습니다.");
        return "redirect:/admin/courses";
    }

    // --- 커리큘럼 관리 ---

    /**
     * 커리큘럼 관리 페이지
     */
    @GetMapping("/{courseId}/curriculum")
    public String curriculumManagement(@PathVariable Long courseId, Model model) {
        // 서비스 계층을 통해 필요한 모든 데이터를 DTO 형태로 한번에 조회함.
        CourseCurriculumResponse curriculum = adminCourseService.getCourseCurriculum(courseId);

        model.addAttribute("curriculum", curriculum);
        model.addAttribute("chapterRequest", new ChapterRequest("", 0));
        model.addAttribute("lectureRequest", new LectureRequest("", "", 0, false, null));

        return "admin/curriculum_management";
    }

    /**
     * 신규 챕터 추가 처리
     */
    @PostMapping("/{courseId}/chapters")
    public String addChapter(@PathVariable Long courseId,
                             @Valid @ModelAttribute("chapterRequest") ChapterRequest request,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal User admin,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // 유효성 검증 실패 시, 커리큘럼 페이지를 다시 렌더링
            Course course = courseRepository.findById(courseId).orElseThrow();
            model.addAttribute("course", course);
            model.addAttribute("lectureRequest", new LectureRequest("", "", 0, false, null)); // 빈 렉처 폼도 다시 전달
            return "admin/curriculum_management";
        }
        adminCourseService.addChapterToCourse(courseId, request, admin);
        redirectAttributes.addFlashAttribute("successMessage", "새로운 챕터가 추가되었습니다.");
        return "redirect:/admin/courses/" + courseId + "/curriculum";
    }

    @PostMapping("/chapters/{chapterId}/edit")
    public String updateChapter(@PathVariable Long chapterId,
                                @RequestParam Long courseId,
                                @RequestParam String title,
                                @RequestParam int order,
                                @AuthenticationPrincipal User admin,
                                RedirectAttributes redirectAttributes) {
        if (title == null || title.isBlank() || order < 1) {
            redirectAttributes.addFlashAttribute("errorMessage", "챕터 제목과 순서를 올바르게 입력해주세요.");
            return "redirect:/admin/courses/" + courseId + "/curriculum";
        }
        ChapterRequest request = new ChapterRequest(title, order);
        adminCourseService.updateChapter(chapterId, request, admin);
        redirectAttributes.addFlashAttribute("successMessage", "챕터 정보가 성공적으로 수정되었습니다.");
        return "redirect:/admin/courses/" + courseId + "/curriculum";
    }

    /**
     * 챕터 삭제 처리
     */
    @PostMapping("/chapters/{chapterId}/delete")
    public String deleteChapter(@PathVariable Long chapterId,
                                @RequestParam Long courseId,
                                @AuthenticationPrincipal User admin,
                                RedirectAttributes redirectAttributes) {
        adminCourseService.deleteChapter(chapterId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "챕터가 성공적으로 삭제되었습니다.");
        return "redirect:/admin/courses/" + courseId + "/curriculum";
    }

    /**
     * 신규 렉처 추가 처리
     */
    @PostMapping("/chapters/{chapterId}/lectures")
    public String addLecture(@PathVariable Long chapterId,
                             @Valid @ModelAttribute("lectureRequest") LectureRequest request,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal User admin,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        // 원래 페이지로 돌아가기 위해 courseId 필요
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 챕터입니다."));
        Long courseId = chapter.getCourse().getId();

        if (bindingResult.hasErrors()) {
            Course course = courseRepository.findById(courseId).orElseThrow();
            model.addAttribute("course", course);
            model.addAttribute("chapterRequest", new ChapterRequest("", 0));
            return "admin/curriculum_management";
        }
        adminCourseService.addLecture(chapterId, request, admin);
        redirectAttributes.addFlashAttribute("successMessage", "새로운 렉처가 추가되었습니다.");
        return "redirect:/admin/courses/" + courseId + "/curriculum";
    }

    /**
     * 렉처 수정 폼 페이지
     */
    @GetMapping("/lectures/{lectureId}/edit")
    public String editLectureForm(@PathVariable Long lectureId, Model model) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 렉처입니다."));

        LectureRequest request = new LectureRequest(
                lecture.getTitle(),
                lecture.getVideoUrl(),
                lecture.getOrder(),
                lecture.isSample(),
                lecture.getDurationSeconds()
        );

        model.addAttribute("lectureRequest", request);
        model.addAttribute("lectureId", lectureId);
        // 수정 완료 후 돌아갈 courseId도 전달
        model.addAttribute("courseId", lecture.getChapter().getCourse().getId());
        return "admin/lecture_form";
    }

    /**
     * 렉처 수정 처리
     */
    @PostMapping("/lectures/{lectureId}/edit")
    public String updateLecture(@PathVariable Long lectureId,
                                @Valid @ModelAttribute("lectureRequest") LectureRequest request,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal User admin,
                                RedirectAttributes redirectAttributes) {

        Long courseId = lectureRepository.findById(lectureId).orElseThrow().getChapter().getCourse().getId();

        if (bindingResult.hasErrors()) {
            return "admin/lecture_form";
        }
        adminCourseService.updateLecture(lectureId, request, admin);
        redirectAttributes.addFlashAttribute("successMessage", "렉처 정보가 수정되었습니다.");
        return "redirect:/admin/courses/" + courseId + "/curriculum";
    }

    /**
     * 렉처 삭제 처리
     */
    @PostMapping("/lectures/{lectureId}/delete")
    public String deleteLecture(@PathVariable Long lectureId, @AuthenticationPrincipal User admin, RedirectAttributes redirectAttributes) {
        Long courseId = lectureRepository.findById(lectureId).orElseThrow().getChapter().getCourse().getId();
        adminCourseService.deleteLecture(lectureId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "렉처가 삭제되었습니다.");
        return "redirect:/admin/courses/" + courseId + "/curriculum";
    }

    /**
     * 신규 과제 등록 폼 페이지
     */
    @GetMapping("/lectures/{lectureId}/assignments/new")
    public String newAssignmentForm(@PathVariable Long lectureId, Model model) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 렉처입니다."));
        model.addAttribute("lecture", lecture);
        model.addAttribute("assignmentRequest", new AssignmentCreateRequest(null, null, null));
        return "admin/assignment_form";
    }

    /**
     * 신규 과제 생성 처리
     */
    @PostMapping("/lectures/{lectureId}/assignments")
    public String createAssignment(@PathVariable Long lectureId,
                                   @Valid @ModelAttribute("assignmentRequest") AssignmentCreateRequest request,
                                   BindingResult bindingResult,
                                   @AuthenticationPrincipal User admin,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 렉처입니다."));

        if (bindingResult.hasErrors()) {
            model.addAttribute("lecture", lecture);
            return "admin/assignment_form";
        }

        adminCourseService.createAssignmentForLecture(lectureId, request, admin);
        redirectAttributes.addFlashAttribute("successMessage", "새로운 과제가 성공적으로 등록되었습니다.");

        return "redirect:/admin/courses/" + lecture.getChapter().getCourse().getId() + "/curriculum";
    }

    /**
     * 과제 수정 폼 페이지
     */
    @GetMapping("/assignments/{assignmentId}/edit")
    public String editAssignmentForm(@PathVariable Long assignmentId, Model model) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과제입니다."));

        AssignmentUpdateRequest request = new AssignmentUpdateRequest(
                assignment.getTitle(),
                assignment.getContent(),
                assignment.getDeadline() != null ? LocalDateTime.ofInstant(assignment.getDeadline(), ZoneOffset.UTC) : null
        );

        model.addAttribute("assignment", assignment);
        model.addAttribute("assignmentRequest", request);
        return "admin/assignment_edit_form";
    }

    /**
     * 과제 수정 처리
     */
    @PostMapping("/assignments/{assignmentId}/edit")
    public String updateAssignment(@PathVariable Long assignmentId,
                                   @Valid @ModelAttribute("assignmentRequest") AssignmentUpdateRequest request,
                                   BindingResult bindingResult,
                                   @AuthenticationPrincipal User admin,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과제입니다."));

        if (bindingResult.hasErrors()) {
            model.addAttribute("assignment", assignment);
            return "admin/assignment_edit_form";
        }

        adminCourseService.updateAssignment(assignmentId, request, admin);
        redirectAttributes.addFlashAttribute("successMessage", "과제가 성공적으로 수정되었습니다.");
        return "redirect:/admin/courses/" + assignment.getLecture().getChapter().getCourse().getId() + "/curriculum";
    }

    /**
     * 과제 삭제 처리
     */
    @PostMapping("/assignments/{assignmentId}/delete")
    public String deleteAssignment(@PathVariable Long assignmentId,
                                   @RequestParam Long courseId,
                                   @AuthenticationPrincipal User admin,
                                   RedirectAttributes redirectAttributes) {
        adminCourseService.deleteAssignment(assignmentId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "과제가 성공적으로 삭제되었습니다.");
        return "redirect:/admin/courses/" + courseId + "/curriculum";
    }
}