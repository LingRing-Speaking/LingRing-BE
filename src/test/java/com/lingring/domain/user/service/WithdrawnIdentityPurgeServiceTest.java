package com.lingring.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.user.dao.WithdrawnIdentityRepository;
import com.lingring.domain.user.domain.WithdrawnIdentity;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WithdrawnIdentityPurgeServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private WithdrawnIdentityPurgeService withdrawnIdentityPurgeService;

    @Autowired
    private WithdrawnIdentityRepository withdrawnIdentityRepository;

    @Autowired
    private DateTimeProvider dateTimeProvider;

    @Nested
    @DisplayName("purgeExpired: 보관 기간이 지난 탈퇴 계정 해시 파기")
    class PurgeExpired {

        @Test
        @DisplayName("탈퇴 후 1년이 지난 해시는 파기된다")
        void purgeExpired_whenOlderThanOneYear_deletesHash() {
            // given
            final LocalDateTime oneYearAgo = dateTimeProvider.now().minusYears(1);
            withdrawnIdentityRepository.save(WithdrawnIdentity.record("expired", oneYearAgo.minusDays(1)));

            // when
            final int purged = withdrawnIdentityPurgeService.purgeExpired();

            // then
            assertThat(purged).isEqualTo(1);
            assertThat(withdrawnIdentityRepository.existsByIdentityHash("expired")).isFalse();
        }

        @Test
        @DisplayName("탈퇴 후 1년이 지나지 않은 해시는 유지된다")
        void purgeExpired_whenWithinOneYear_keepsHash() {
            // given
            final LocalDateTime oneYearAgo = dateTimeProvider.now().minusYears(1);
            withdrawnIdentityRepository.save(WithdrawnIdentity.record("retained", oneYearAgo.plusDays(1)));

            // when
            final int purged = withdrawnIdentityPurgeService.purgeExpired();

            // then
            assertThat(purged).isZero();
            assertThat(withdrawnIdentityRepository.existsByIdentityHash("retained")).isTrue();
        }
    }
}
