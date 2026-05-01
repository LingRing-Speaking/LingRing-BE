package com.lingring.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProviderTest {

    @Nested
    @DisplayName("from: 정상 매칭")
    class HappyPath {

        @ParameterizedTest
        @ValueSource(strings = {"kakao", "KAKAO", "Kakao", "kAkAo"})
        @DisplayName("대소문자 구분 없이 KAKAO를 반환한다")
        void from_whenKakaoVariants_returnsKakao(final String raw) {
            // when
            final Provider provider = Provider.from(raw);

            // then
            assertThat(provider).isEqualTo(Provider.KAKAO);
        }

        @ParameterizedTest
        @ValueSource(strings = {"apple", "APPLE", "Apple"})
        @DisplayName("대소문자 구분 없이 APPLE을 반환한다")
        void from_whenAppleVariants_returnsApple(final String raw) {
            // when
            final Provider provider = Provider.from(raw);

            // then
            assertThat(provider).isEqualTo(Provider.APPLE);
        }
    }

    @Nested
    @DisplayName("from: 입력 검증")
    class Validation {

        @Test
        @DisplayName("null이면 INVALID_INPUT_VALUE 예외를 던진다")
        void from_whenNull_throwsInvalidInputValue() {
            // when & then
            assertThatThrownBy(() -> Provider.from(null))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t"})
        @DisplayName("공백 문자열이면 INVALID_INPUT_VALUE 예외를 던진다")
        void from_whenBlank_throwsInvalidInputValue(final String raw) {
            // when & then
            assertThatThrownBy(() -> Provider.from(raw))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"facebook", "google", "naver", "unknown"})
        @DisplayName("미지원 provider면 NOT_SUPPORTED 예외를 던진다")
        void from_whenUnsupported_throwsNotSupported(final String raw) {
            // when & then
            assertThatThrownBy(() -> Provider.from(raw))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_SUPPORTED);
        }

        @Test
        @DisplayName("미지원 provider 예외 메시지에 raw 입력값이 포함된다")
        void from_whenUnsupported_messageContainsRawInput() {
            // when & then
            assertThatThrownBy(() -> Provider.from("facebook"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("facebook");
        }
    }
}