package com.lingring.domain.push.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlatformTest {

    @Nested
    @DisplayName("from: platform 문자열 파싱")
    class From {

        @ParameterizedTest
        @ValueSource(strings = {"ios", "IOS", "iOS"})
        @DisplayName("대소문자와 무관하게 IOS로 매핑한다")
        void from_whenIosIgnoringCase_returnsIos(final String raw) {
            // when & then
            assertThat(Platform.from(raw)).isEqualTo(Platform.IOS);
        }

        @Test
        @DisplayName("소문자 android를 ANDROID로 매핑한다")
        void from_whenLowerAndroid_returnsAndroid() {
            // when & then
            assertThat(Platform.from("android")).isEqualTo(Platform.ANDROID);
        }

        @Test
        @DisplayName("지원하지 않는 값이면 NOT_SUPPORTED 예외")
        void from_whenUnknown_throwsBadRequest() {
            // when & then
            assertThatThrownBy(() -> Platform.from("web"))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_SUPPORTED);
        }

        @Test
        @DisplayName("빈 문자열이면 INVALID_INPUT_VALUE 예외")
        void from_whenBlank_throwsBadRequest() {
            // when & then
            assertThatThrownBy(() -> Platform.from(" "))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
