package com.lingring.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AgreementItemTest {

    @Nested
    @DisplayName("required: 필수 동의 항목")
    class Required {

        @Test
        @DisplayName("OVER14·TERMS·PRIVACY·VOICE_AI만 필수이고 MARKETING_PUSH는 선택 항목이다")
        void required_excludesMarketingPush() {
            // when & then
            assertThat(AgreementItem.required()).containsExactlyInAnyOrder(
                    AgreementItem.OVER14,
                    AgreementItem.TERMS,
                    AgreementItem.PRIVACY,
                    AgreementItem.VOICE_AI
            );
        }
    }

    @Nested
    @DisplayName("fromString: 약관 항목 파싱")
    class FromString {

        @Test
        @DisplayName("소문자 snake_case marketing_push를 MARKETING_PUSH로 매핑한다")
        void fromString_whenLowerSnakeCase_returnsMarketingPush() {
            // when & then
            assertThat(AgreementItem.fromString("marketing_push")).isEqualTo(AgreementItem.MARKETING_PUSH);
        }
    }
}
