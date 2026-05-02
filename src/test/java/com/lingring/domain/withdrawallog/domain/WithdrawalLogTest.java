package com.lingring.domain.withdrawallog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.domain.WithdrawReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WithdrawalLogTest {

    @Test
    @DisplayName("OTHER 사유로 생성하면 description이 VO로 보관된다")
    void record_whenReasonIsOther_keepsDescription() {
        // given
        final String description = "더 이상 사용할 일이 없어요";

        // when
        final WithdrawalLog log = WithdrawalLog.record(WithdrawReason.OTHER, description);

        // then
        assertThat(log.getReason()).isEqualTo(WithdrawReason.OTHER);
        assertThat(log.getDescription()).isNotNull();
        assertThat(log.getDescription().getValue()).isEqualTo(description);
    }

    @Test
    @DisplayName("OTHER가 아닌 사유로 생성하면 description이 들어와도 무시되고 null이 된다")
    void record_whenReasonIsNotOther_ignoresDescription() {
        // when
        final WithdrawalLog log = WithdrawalLog.record(WithdrawReason.NO_GOOD_MATCH, "이건 무시되어야 함");

        // then
        assertThat(log.getReason()).isEqualTo(WithdrawReason.NO_GOOD_MATCH);
        assertThat(log.getDescription()).isNull();
    }

    @Test
    @DisplayName("OTHER가 아닌 사유로 생성 시 description이 null이어도 정상적으로 생성된다")
    void record_whenReasonIsNotOtherAndDescriptionIsNull_returnsLogWithNullDescription() {
        // when
        final WithdrawalLog log = WithdrawalLog.record(WithdrawReason.BUGGY, null);

        // then
        assertThat(log.getReason()).isEqualTo(WithdrawReason.BUGGY);
        assertThat(log.getDescription()).isNull();
    }

    @Test
    @DisplayName("OTHER 사유인데 description이 null이면 NPE가 발생한다 (@NonNull 가드)")
    void record_whenReasonIsOtherAndDescriptionIsNull_throwsNullPointerException() {
        // when & then
        assertThatThrownBy(() -> WithdrawalLog.record(WithdrawReason.OTHER, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("reason이 null이면 NPE가 발생한다")
    void record_whenReasonIsNull_throwsNullPointerException() {
        // when & then
        assertThatThrownBy(() -> WithdrawalLog.record(null, "anything"))
                .isInstanceOf(NullPointerException.class);
    }
}
