package com.platform.coding.domain.course;

import com.platform.coding.domain.submission.Assignment; // Assignment 임포트 추가
import com.platform.coding.domain.submission.Submission;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map; // Map 임포트 추가

// 강의 상세 조회용 DTO
public record DetailedCourseResponse(
        Long courseId,
        String title,
        String description,
        String instructorName,
        BigDecimal price,
        List<ChapterDTO> curriculum
) {
    @Builder
    public DetailedCourseResponse {}

    // 내부 DTO: Chapter 정보를 담음
    public record ChapterDTO(
            Long chapterId,
            String title,
            List<LectureDTO> lectures
    ) {
        @Builder
        public ChapterDTO {}
    }

    // 내부 DTO: Lecture 정보에 과제 목록(assignments) 추가
    public record LectureDTO(
            Long lectureId,
            String title,
            boolean isSample,
            List<AssignmentDTO> assignments
    ) {
        @Builder
        public LectureDTO {}
    }

    // 내부 DTO: Assignment 정보를 담음
    public record AssignmentDTO(
            Long assignmentId,
            String title,
            Long submissionId,
            String submissionStatus
    ) {
        @Builder
        public AssignmentDTO {}
    }


    /**
     * 과제 목록을 포함하여 DTO를 생성하도록 로직 변경
     */
    public static DetailedCourseResponse fromEntity(Course course, Map<Long, List<Assignment>> assignmentsByLecture, Map<Long, Submission> submissionsByAssignment) {
        List<ChapterDTO> chapterDTOS = course.getChapters().stream()
                .map(chapter -> new ChapterDTO(
                        chapter.getId(),
                        chapter.getTitle(),
                        chapter.getLectures().stream()
                                .map(lecture -> new LectureDTO(
                                        lecture.getId(),
                                        lecture.getTitle(),
                                        lecture.isSample(),
                                        // 각 렉처에 해당하는 과제 목록을 DTO로 변환
                                        assignmentsByLecture.getOrDefault(lecture.getId(), List.of()).stream()
                                                .map(assignment -> {
                                                    // 해당 과제에 대한 학생의 제출 정보를 조회
                                                    Submission submission = submissionsByAssignment.get(assignment.getId());
                                                    return new AssignmentDTO(
                                                            assignment.getId(),
                                                            assignment.getTitle(),
                                                            // 제출 정보가 있으면 ID와 상태를, 없으면 null을 DTO에 담음
                                                            submission != null ? submission.getId() : null,
                                                            submission != null ? submission.getStatus().name() : null
                                                    );
                                                })
                                                .toList()
                                ))
                                .toList()
                ))
                .toList();

        return new DetailedCourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getAdmin().getUserName(),
                course.getPrice(),
                chapterDTOS
        );
    }
}