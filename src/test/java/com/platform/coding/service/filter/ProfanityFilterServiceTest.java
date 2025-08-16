package com.platform.coding.service.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class ProfanityFilterServiceTest {
    private final ProfanityFilterService profanityFilterService = new ProfanityFilterService();

    @DisplayName("금지어가 포함된 텍스트는 true를 반환해야 한다.")
    @ParameterizedTest
    @ValueSource(strings = {
            "이 강의는 완전 바보 같아요.",
            "정말 멍청이 같은 설명이네요.",
            "이런 씨발같은 강의가 있나.",
            "광고가 너무 많아요.",
            "https://evil-site.com 링크를 첨부합니다."
    })
    void containsProfanity(String text) {
        assertThat(profanityFilterService.containsProfanity(text)).isTrue();
    }

    @DisplayName("정상적인 텍스트는 false를 반환해야 한다.")
    @ParameterizedTest
    @ValueSource(strings = {
            "정말 좋은 강의입니다.",
            "많은 것을 배워갑니다.",
            "강사님의 설명이 귀에 쏙쏙 들어와요."
    })
    void doesNotContainProfanity(String text) {
        assertThat(profanityFilterService.containsProfanity(text)).isFalse();
    }

    @DisplayName("공백이나 대소문자가 섞여있어도 금지어를 탐지해야 한다.")
    @ParameterizedTest
    @ValueSource(strings = {
            "이런 씨 발 같은 강의가 있나.", // 공백 포함
            "완전 멍 청 이 같은 설명", // 공백 포함
            "Jonna 어렵네요.", // 대소문자 혼용
            "WWW.SPAMSITE.COM" // 대소문자 혼용
    })
    void containsProfanityWithWhitespaceAndMixedCase(String text) {
        assertThat(profanityFilterService.containsProfanity(text)).isTrue();
    }

    @DisplayName("null 또는 비어있는 텍스트는 false를 반환해야 한다.")
    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void returnsFalseForEmptyOrNullText(String text) {
        assertThat(profanityFilterService.containsProfanity(text)).isFalse();
        assertThat(profanityFilterService.containsProfanity(null)).isFalse();
    }
}
