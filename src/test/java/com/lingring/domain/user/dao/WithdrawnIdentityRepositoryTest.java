package com.lingring.domain.user.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.user.domain.WithdrawnIdentity;
import com.lingring.global.config.RepositoryTestHelper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WithdrawnIdentityRepositoryTest extends RepositoryTestHelper {

    private static final LocalDateTime THRESHOLD = LocalDateTime.of(2026, 3, 1, 4, 0);

    @Autowired
    private WithdrawnIdentityRepository withdrawnIdentityRepository;

    @Nested
    @DisplayName("deleteAllWithdrawnBefore")
    class DeleteAllWithdrawnBefore {

        @Test
        @DisplayName("기준 시각 이전에 탈퇴한 행만 삭제하고 삭제 건수를 반환한다")
        void deleteAllWithdrawnBefore_deletesOnlyOlderRows() {
            // given
            withdrawnIdentityRepository.save(WithdrawnIdentity.record("expired", THRESHOLD.minusSeconds(1)));
            withdrawnIdentityRepository.save(WithdrawnIdentity.record("kept", THRESHOLD.plusSeconds(1)));

            // when
            final int purged = withdrawnIdentityRepository.deleteAllWithdrawnBefore(THRESHOLD);

            // then
            assertThat(purged).isEqualTo(1);
            assertThat(withdrawnIdentityRepository.existsByIdentityHash("expired")).isFalse();
            assertThat(withdrawnIdentityRepository.existsByIdentityHash("kept")).isTrue();
        }

        @Test
        @DisplayName("기준 시각과 정확히 같은 시각에 탈퇴한 행은 삭제하지 않는다")
        void deleteAllWithdrawnBefore_whenExactlyThreshold_keepsRow() {
            // given
            withdrawnIdentityRepository.save(WithdrawnIdentity.record("boundary", THRESHOLD));

            // when
            final int purged = withdrawnIdentityRepository.deleteAllWithdrawnBefore(THRESHOLD);

            // then
            assertThat(purged).isZero();
            assertThat(withdrawnIdentityRepository.existsByIdentityHash("boundary")).isTrue();
        }
    }
}
