package com.lingring.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.review.dao.AnalysisQuotaRepository;
import com.lingring.domain.review.domain.quota.AnalysisQuota;
import com.lingring.domain.review.dto.response.AnalysisQuotaResponse;
import com.lingring.domain.review.exception.AnalysisQuotaExhaustedException;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.util.FixedDateTimeProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import(AnalysisQuotaServiceTest.FixedDateTimeProviderConfig.class)
class AnalysisQuotaServiceTest extends ServiceIntegrationHelper {

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 28);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    @Autowired
    private AnalysisQuotaService analysisQuotaService;

    @Autowired
    private AnalysisQuotaRepository analysisQuotaRepository;

    @Autowired
    private FixedDateTimeProvider fixedDateTimeProvider;

    @TestConfiguration
    static class FixedDateTimeProviderConfig {

        @Bean
        @Primary
        FixedDateTimeProvider dateTimeProvider() {
            return new FixedDateTimeProvider(TODAY.atStartOfDay());
        }
    }

    private void setDate(final LocalDate date) {
        fixedDateTimeProvider.setFixedTime(date.atStartOfDay());
    }

    @Nested
    @DisplayName("consumeFreeDaily: 무료 → 유료 차감 (lazy 생성)")
    class ConsumeFreeDaily {

        @Test
        @DisplayName("쿼터 행이 없으면 새로 만들고 무료를 차감한다")
        void consumeFreeDaily_whenNoRow_createsRowAndConsumesFree() {
            // given
            setDate(TODAY);

            // when
            analysisQuotaService.consumeFreeDaily(USER_ID);

            // then
            final AnalysisQuota quota = analysisQuotaRepository.findByUserId(USER_ID).orElseThrow();
            assertThat(quota.freeRemainingOn(TODAY)).isZero();
        }

        @Test
        @DisplayName("같은 날 무료를 모두 쓰고 유료가 없으면 예외를 던진다")
        void consumeFreeDaily_whenExhausted_throws() {
            // given
            setDate(TODAY);
            analysisQuotaService.consumeFreeDaily(USER_ID);

            // when & then
            assertThatThrownBy(() -> analysisQuotaService.consumeFreeDaily(USER_ID))
                    .isInstanceOf(AnalysisQuotaExhaustedException.class);
        }

        @Test
        @DisplayName("날이 바뀌면 무료가 리필되어 다시 차감할 수 있다")
        void consumeFreeDaily_whenNewDay_consumesAgain() {
            // given
            setDate(TODAY);
            analysisQuotaService.consumeFreeDaily(USER_ID);

            // when
            setDate(TOMORROW);
            analysisQuotaService.consumeFreeDaily(USER_ID);

            // then
            final AnalysisQuota quota = analysisQuotaRepository.findByUserId(USER_ID).orElseThrow();
            assertThat(quota.freeResetDate()).isEqualTo(TOMORROW);
            assertThat(quota.freeRemainingOn(TOMORROW)).isZero();
        }
    }

    @Nested
    @DisplayName("getStatus: 잔여 조회 (행 생성 안 함)")
    class GetStatus {

        @Test
        @DisplayName("쿼터 행이 없으면 무료 한도/유료 0을 반환하고 행을 만들지 않는다")
        void getStatus_whenNoRow_returnsLimitAndDoesNotCreateRow() {
            // given
            setDate(TODAY);

            // when
            final AnalysisQuotaResponse response = analysisQuotaService.getStatus(USER_ID);

            // then
            assertThat(response.freeTicket()).isEqualTo(1);
            assertThat(response.paidTicket()).isZero();
            assertThat(response.nextResetAt()).isEqualTo(LocalDateTime.of(2026, 6, 29, 0, 0));
            assertThat(analysisQuotaRepository.findByUserId(USER_ID)).isEmpty();
        }

        @Test
        @DisplayName("무료를 소진했으면 freeTicket 0을 반환한다")
        void getStatus_whenFreeConsumed_returnsZeroFree() {
            // given
            setDate(TODAY);
            analysisQuotaService.consumeFreeDaily(USER_ID);

            // when
            final AnalysisQuotaResponse response = analysisQuotaService.getStatus(USER_ID);

            // then
            assertThat(response.freeTicket()).isZero();
        }

        @Test
        @DisplayName("유료 티켓이 충전돼 있으면 paidTicket을 반환한다")
        void getStatus_whenPaidCharged_returnsPaid() {
            // given
            setDate(TODAY);
            final AnalysisQuota charged = AnalysisQuota.initial(USER_ID);
            charged.charge(5);
            analysisQuotaRepository.save(charged);

            // when
            final AnalysisQuotaResponse response = analysisQuotaService.getStatus(USER_ID);

            // then
            assertThat(response.freeTicket()).isEqualTo(1);
            assertThat(response.paidTicket()).isEqualTo(5);
        }
    }
}
