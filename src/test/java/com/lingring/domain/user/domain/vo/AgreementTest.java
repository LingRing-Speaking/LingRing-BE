package com.lingring.domain.user.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AgreementTest {

    private static final LocalDateTime FIXED_AT = LocalDateTime.of(2026, 5, 6, 12, 0);

    @Nested
    @DisplayName("of: 약관 동의 VO 생성")
    class Of {

        @Test
        @DisplayName("정상 버전과 시각이 주어지면 두 값을 그대로 보관한다")
        void of_whenValid_holdsValues() {
            // when
            final Agreement agreement = Agreement.of("2026-05-06", FIXED_AT);

            // then
            assertThat(agreement.getTermsVersion()).isEqualTo("2026-05-06");
            assertThat(agreement.getAgreedAt()).isEqualTo(FIXED_AT);
        }

        @Test
        @DisplayName("termsVersion이 빈 문자열이면 INVALID_INPUT_VALUE 예외")
        void of_whenBlankVersion_throwsInvalidValue() {
            // when & then
            assertThatThrownBy(() -> Agreement.of("   ", FIXED_AT))
                    .isInstanceOf(InvalidValueException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("termsVersion이 20자를 초과하면 INVALID_INPUT_VALUE 예외")
        void of_whenVersionTooLong_throwsInvalidValue() {
            // given
            final String tooLong = "a".repeat(21);

            // when & then
            assertThatThrownBy(() -> Agreement.of(tooLong, FIXED_AT))
                    .isInstanceOf(InvalidValueException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("termsVersion이 null이면 NPE")
        void of_whenNullVersion_throwsNpe() {
            // when & then
            assertThatThrownBy(() -> Agreement.of(null, FIXED_AT))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("agreedAt이 null이면 NPE")
        void of_whenNullAgreedAt_throwsNpe() {
            // when & then
            assertThatThrownBy(() -> Agreement.of("2026-05-06", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}