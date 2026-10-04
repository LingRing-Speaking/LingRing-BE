package com.lingring.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WithdrawnIdentityTest {

    @Test
    @DisplayName("renew하면 탈퇴 시각이 새 시각으로 갱신된다")
    void renew_updatesWithdrawnAt() {
        // given
        final WithdrawnIdentity identity = WithdrawnIdentity.record(
                "hash", LocalDateTime.of(2026, 3, 1, 14, 0)
        );
        final LocalDateTime rewithdrawnAt = LocalDateTime.of(2026, 5, 1, 9, 30);

        // when
        identity.renew(rewithdrawnAt);

        // then
        assertThat(identity.getWithdrawnAt()).isEqualTo(rewithdrawnAt);
    }

    @Test
    @DisplayName("파기 기준 시각은 현재 시각에서 보관 기간 1년을 뺀 시각이다")
    void expirationThreshold_returnsOneYearBefore() {
        // given
        final LocalDateTime now = LocalDateTime.of(2027, 3, 1, 4, 0);

        // when
        final LocalDateTime threshold = WithdrawnIdentity.expirationThreshold(now);

        // then
        assertThat(threshold).isEqualTo(LocalDateTime.of(2026, 3, 1, 4, 0));
    }
}
