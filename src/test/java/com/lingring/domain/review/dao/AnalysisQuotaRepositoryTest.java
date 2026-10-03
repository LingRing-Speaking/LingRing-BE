package com.lingring.domain.review.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.review.domain.quota.AnalysisQuota;
import com.lingring.global.config.RepositoryTestHelper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnalysisQuotaRepositoryTest extends RepositoryTestHelper {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 14, 0);

    @Autowired
    private AnalysisQuotaRepository analysisQuotaRepository;

    @Nested
    @DisplayName("addPaidTicket")
    class AddPaidTicket {

        @Test
        @DisplayName("쿼터 행이 있으면 유료 티켓을 더하고 1을 반환한다")
        void addPaidTicket_whenRowExists_increasesAndReturnsOne() {
            // given
            analysisQuotaRepository.save(AnalysisQuota.initial(1L));

            // when
            final int updated = analysisQuotaRepository.addPaidTicket(1L, 3, NOW);

            // then
            assertThat(updated).isEqualTo(1);
            assertThat(analysisQuotaRepository.findByUserId(1L).orElseThrow().paidRemaining()).isEqualTo(3);
        }

        @Test
        @DisplayName("쿼터 행이 없으면 0을 반환하고 행을 만들지 않는다")
        void addPaidTicket_whenNoRow_returnsZero() {
            // when
            final int updated = analysisQuotaRepository.addPaidTicket(1L, 3, NOW);

            // then
            assertThat(updated).isZero();
            assertThat(analysisQuotaRepository.findByUserId(1L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdForUpdate")
    class FindByUserIdForUpdate {

        @Test
        @DisplayName("userId의 쿼터 행을 조회한다")
        void findByUserIdForUpdate_whenRowExists_returnsQuota() {
            // given
            final AnalysisQuota saved = analysisQuotaRepository.save(AnalysisQuota.initial(1L));

            // when & then
            assertThat(analysisQuotaRepository.findByUserIdForUpdate(1L))
                    .map(AnalysisQuota::getId)
                    .contains(saved.getId());
        }

        @Test
        @DisplayName("쿼터 행이 없으면 빈 Optional을 반환한다")
        void findByUserIdForUpdate_whenNoRow_returnsEmpty() {
            // when & then
            assertThat(analysisQuotaRepository.findByUserIdForUpdate(1L)).isEmpty();
        }
    }
}
