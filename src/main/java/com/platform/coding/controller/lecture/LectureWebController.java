package com.platform.coding.controller.lecture;

import com.platform.coding.domain.course.Lecture;
import com.platform.coding.domain.course.LectureRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.lecture.LectureService;
import com.platform.coding.service.lecture.dto.LectureWebResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/lectures")
@RequiredArgsConstructor
public class LectureWebController {
    private final LectureService lectureService;
    private final LectureRepository lectureRepository;

    /**
     * 동영상 시청 페이지를 반환합니다.
     * 이 메소드 호출 전에 Spring Security가 사용자의 인증 여부를 확인합니다.
     */
    @GetMapping("/{lectureId}/watch")
    public String watchLecturePage(@PathVariable Long lectureId, @AuthenticationPrincipal User student, Model model) {
        // 서비스 계층에서 수강 여부 등 권한 확인 후, 뷰에 필요한 데이터를 DTO로 받아옵니다.
        LectureWebResponse response = lectureService.getLectureForWeb(lectureId, student);
        model.addAttribute("lecture", response);
        return "lecture/view";
    }

    /**
     * 이 컨트롤러 내에서 AccessDeniedException이 발생하면 이 메소드가 처리합니다.
     * @param e 발생한 예외 객체
     * @param redirectAttributes 리다이렉트 시 메시지를 전달하기 위한 객체
     * @return 리다이렉트할 URL
     */
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        // 사용자에게 보여줄 에러 메시지를 RedirectAttributes에 추가합니다.
        // 'flash' 속성은 리다이렉트된 페이지에서 한 번만 사용되고 사라집니다.
        redirectAttributes.addFlashAttribute("errorMessage", "강의를 수강해야 시청할 수 있습니다. 먼저 수강 신청을 진행해주세요.");

        try {
            // 1. 현재 요청된 URL에서 Path Variable({lectureId}) 값을 추출합니다.
            Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            Long lectureId = Long.parseLong(pathVariables.get("lectureId"));

            // 2. lectureId를 사용하여 DB에서 해당 Lecture 엔티티를 찾고, courseId를 알아냅니다.
            Optional<Lecture> optionalLecture = lectureRepository.findById(lectureId);
            if (optionalLecture.isPresent()) {
                Long courseId = optionalLecture.get().getChapter().getCourse().getId();
                // 3. 알아낸 courseId를 사용하여 특정 강의 상세 페이지로 리다이렉트 경로를 생성합니다.
                return "redirect:/courses/" + courseId;
            }
        } catch (Exception ex) {
            // lectureId를 파싱하거나 DB 조회 중 예외가 발생할 경우, 안전하게 기본 강의 목록 페이지로 보냅니다.
            // (정상적인 경우 이 코드는 실행되지 않습니다.)
            log.warn(ex.getMessage());
        }

        // 4. (Fallback) 만약의 경우를 대비해 기본 강의 목록 페이지로 리다이렉트합니다.
        return "redirect:/courses";
    }
}
