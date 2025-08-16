package com.platform.coding.controller.admin;

import com.platform.coding.domain.course.SimpleCourseResponse;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.admin.AdminCourseService;
import com.platform.coding.service.admin.dto.ChapterRequest;
import com.platform.coding.service.admin.dto.CourseCreateRequest;
import com.platform.coding.service.admin.dto.CourseUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {
    private final AdminCourseService adminCourseService;

    // 강의 생성 API
    @PostMapping
    public ResponseEntity<SimpleCourseResponse> createCourse(
            @Valid @RequestBody CourseCreateRequest request,
            @AuthenticationPrincipal User admin
    ) {
        SimpleCourseResponse response = adminCourseService.createCourse(request, admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 강의 수정 API
    @PutMapping("/{courseId}")
    public ResponseEntity<SimpleCourseResponse> updateCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseUpdateRequest request,
            @AuthenticationPrincipal User admin
            ) {
        SimpleCourseResponse response = adminCourseService.updateCourse(courseId, request, admin);
        return ResponseEntity.ok(response);
    }

    // 강의 삭제(보관) API
    @DeleteMapping("{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User admin
    ) {
        adminCourseService.archiveCourse(courseId, admin);
        return ResponseEntity.noContent().build();
    }
}
