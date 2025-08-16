package com.platform.coding.controller.admin;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.admin.AdminCourseService;
import com.platform.coding.service.admin.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCurriculumController {
    private final AdminCourseService adminCourseService;

    // 챕터 추가
    @PostMapping("/courses/{courseId}/chapters")
    public ResponseEntity<Void> createChapter(
            @PathVariable Long courseId,
            @Valid @RequestBody ChapterRequest request,
            @AuthenticationPrincipal User admin
    ) {
        adminCourseService.addChapterToCourse(courseId, request, admin);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/chapters/{chapterId}")
    public ResponseEntity<Void> updateChapter(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterRequest request,
            @AuthenticationPrincipal User admin) {
        adminCourseService.updateChapter(chapterId, request, admin);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/chapters/{chapterId}")
    public ResponseEntity<Void> deleteChapter(
            @PathVariable Long chapterId,
            @AuthenticationPrincipal User admin
    ) {
        adminCourseService.deleteChapter(chapterId, admin);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/chapters/{chapterId}/lectures")
    public ResponseEntity<Void> addLecture(
            @PathVariable Long chapterId,
            @Valid @RequestBody LectureRequest request,
            @AuthenticationPrincipal User admin
            ) {
        adminCourseService.addLecture(chapterId, request, admin);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/lectures/{lectureId}")
    public ResponseEntity<Void> updateLecture(
            @PathVariable Long lectureId,
            @Valid @RequestBody LectureRequest request,
            @AuthenticationPrincipal User admin
    ) {
        adminCourseService.updateLecture(lectureId, request, admin);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/lectures/{lectureId}")
    public ResponseEntity<Void> deleteLecture(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal User admin
    ) {
        adminCourseService.deleteLecture(lectureId, admin);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 피드백 API
    @PostMapping("/submissions/{submissionId}/feedback")
    public ResponseEntity<Void> createFeedback(
            @PathVariable Long submissionId,
            @Valid @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal User admin
    ) {
        adminCourseService.createFeedbackForSubmission(submissionId, request, admin);

        return ResponseEntity.ok().build();
    }

    /**
     * 특정 렉처에 대해 과제를 생성하는 API
     */
    @PostMapping("/lectures/{lectureId}/assignments")
    public ResponseEntity<Void> createAssignment(
            @PathVariable Long lectureId,
            @Valid @RequestBody AssignmentCreateRequest request,
            @AuthenticationPrincipal User admin
    ) {
        Long assignmentId = adminCourseService.createAssignmentForLecture(lectureId, request, admin);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/assignments/{id}")
                .buildAndExpand(assignmentId)
                .toUri();

        return ResponseEntity.created(location).build();
    }

    /**
     * 특정 과제를 수정하는 API
     */
    @PutMapping("/assignments/{assignmentId}")
    public ResponseEntity<Void> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentUpdateRequest request,
            @AuthenticationPrincipal User admin
    ) {
        adminCourseService.updateAssignment(assignmentId, request, admin);
        return ResponseEntity.ok().build();
    }

    /**
     * 특정 과제를 삭제하는 API
     */
    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal User admin
    ) {
        adminCourseService.deleteAssignment(assignmentId, admin);
        return ResponseEntity.noContent().build();
    }
}
