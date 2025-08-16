package com.platform.coding.service.filter;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProfanityFilterService {
    /**
     * 금지어 목록입니다.
     * 실제 운영 환경에서는 DB나 외부 설정 파일을 통해 관리하는 것이 좋습니다.
     */
    private static final Set<String> FORBIDDEN_WORDS = Stream.of(
            "바보", "멍청이", "개새끼", "씨발", "병신", "존나", "졸라", "jonna",
            "광고", "홍보", "판매", "http://", "https://", "www."
    ).map(String::toLowerCase).collect(Collectors.toSet());

    /**
     * 입력된 텍스트에 금지어가 포함되어 있는지 확인합니다.
     * 이 검사는 대소문자를 구분하지 않습니다.
     *
     * @param text 검사할 텍스트
     * @return 금지어가 포함되어 있으면 true, 그렇지 않으면 false
     */
    public boolean containsProfanity(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        // 1. 모든 공백을 제거합니다.
        String sanitizedText = text.replaceAll("\\s+", "");
        // 2. 소문자로 변환하여 비교합니다.
        String lowerCaseText = sanitizedText.toLowerCase();

        for (String forbiddenWord : FORBIDDEN_WORDS) {
            if (lowerCaseText.contains(forbiddenWord)) {
                return true;
            }
        }
        return false;
    }
}
