package com.lingring.domain.user.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MarketingPushConsentTest {

    private static final LocalDateTime FIRST_AT = LocalDateTime.of(2026, 10, 2, 12, 0);
    private static final LocalDateTime SECOND_AT = LocalDateTime.of(2026, 11, 5, 9, 30);

    @Test
    @DisplayName("none: 미동의 상태이며 처리 시각이 없다")
    void none_returnsNotAgreedWithoutUpdatedAt() {
        // when
        final MarketingPushConsent consent = MarketingPushConsent.none();

        // then
        assertThat(consent.isAgreed()).isFalse();
        assertThat(consent.getUpdatedAt()).isNull();
    }

    @Nested
    @DisplayName("change: 수신 동의 변경")
    class Change {

        @Test
        @DisplayName("미동의에서 동의로 바뀌면 동의 상태와 처리 시각이 기록된다")
        void change_whenAgreeFromNone_recordsAgreedAt() {
            // when
            final MarketingPushConsent changed = MarketingPushConsent.none().change(true, FIRST_AT);

            // then
            assertThat(changed.isAgreed()).isTrue();
            assertThat(changed.getUpdatedAt()).isEqualTo(FIRST_AT);
        }

        @Test
        @DisplayName("동의에서 철회로 바뀌면 처리 시각이 철회 시각으로 갱신된다")
        void change_whenWithdraw_updatesUpdatedAt() {
            // given
            final MarketingPushConsent agreed = MarketingPushConsent.none().change(true, FIRST_AT);

            // when
            final MarketingPushConsent withdrawn = agreed.change(false, SECOND_AT);

            // then
            assertThat(withdrawn.isAgreed()).isFalse();
            assertThat(withdrawn.getUpdatedAt()).isEqualTo(SECOND_AT);
        }

        @Test
        @DisplayName("이미 동의한 상태에서 다시 동의하면 처리 시각을 유지한다")
        void change_whenSameAgreed_keepsUpdatedAt() {
            // given
            final MarketingPushConsent agreed = MarketingPushConsent.none().change(true, FIRST_AT);

            // when
            final MarketingPushConsent unchanged = agreed.change(true, SECOND_AT);

            // then
            assertThat(unchanged.getUpdatedAt()).isEqualTo(FIRST_AT);
        }

        @Test
        @DisplayName("한 번도 동의하지 않은 상태에서 미동의를 보내면 처리 시각은 null로 남는다")
        void change_whenNoneAndNotAgreed_keepsNullUpdatedAt() {
            // when
            final MarketingPushConsent unchanged = MarketingPushConsent.none().change(false, FIRST_AT);

            // then
            assertThat(unchanged.isAgreed()).isFalse();
            assertThat(unchanged.getUpdatedAt()).isNull();
        }

        @Test
        @DisplayName("처리 시각이 null이면 NPE")
        void change_whenChangedAtNull_throwsNpe() {
            // when & then
            assertThatThrownBy(() -> MarketingPushConsent.none().change(true, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
