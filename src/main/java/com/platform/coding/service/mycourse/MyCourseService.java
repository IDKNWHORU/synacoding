package com.platform.coding.service.mycourse;

import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.mycourse.dto.MyCourseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyCourseService {
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public List<MyCourseResponse> getMyCourses(User student) {
        return enrollmentRepository.findByStudent(student)
                .stream()
                .map(MyCourseResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
