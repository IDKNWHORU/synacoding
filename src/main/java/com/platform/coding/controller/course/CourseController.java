package com.platform.coding.controller.course;

import com.platform.coding.domain.course.DetailedCourseResponse;
import com.platform.coding.domain.course.SimpleCourseResponse;
import com.platform.coding.service.course.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    // GET /api/courses?page=0?size=10&sort=createdAt,desc
    @GetMapping
    public ResponseEntity<Page<SimpleCourseResponse>> getAllCourses(
            @PageableDefault(sort = "createdAt") Pageable pageable
            ) {
        Page<SimpleCourseResponse> courses = courseService.getAllPublishedCourses(pageable);
        return ResponseEntity.ok(courses);
    }

    // GET /api/courses/1
    @GetMapping("/{courseId}")
    public ResponseEntity<DetailedCourseResponse> getCourseDetails (@PathVariable Long courseId) {
        DetailedCourseResponse courseDetails = courseService.getCourseDetails(courseId);
        return ResponseEntity.ok(courseDetails);
    }
}
