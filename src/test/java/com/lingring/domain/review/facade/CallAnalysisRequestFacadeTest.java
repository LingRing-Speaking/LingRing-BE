package com.lingring.domain.review.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.review.dao.CallRecordingRepository;
import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.review.dao.CallTranscriptRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.review.domain.recording.CallRecording;
import com.lingring.domain.call.exception.CallParticipantMismatchException;
import com.lingring.domain.review.exception.CallRecordingExpiredException;
import com.lingring.domain.review.exception.CallRecordingsNotReadyException;
import com.lingring.domain.review.dao.CallAnalysisRepository;
import com.lingring.domain.review.dao.AnalysisQuotaRepository;
import com.lingring.domain.review.domain.quota.AnalysisQuota;
import com.lingring.domain.review.domain.port.CallAnalysisStarter;
import com.lingring.domain.review.domain.analysis.CallAnalysisStatus;
import com.lingring.domain.review.dto.response.CallAnalysisStartResponse;
import com.lingring.domain.review.exception.AnalysisQuotaExhaustedException;
import com.lingring.domain.review.support.FakeCallAnalysisStarter;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.util.FixedDateTimeProvider;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import(CallAnalysisRequestFacadeTest.FakeStarterConfig.class)
class CallAnalysisRequestFacadeTest extends ServiceIntegrationHelper {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 28);
    private static final LocalDateTime ENDED_AT = LocalDateTime.of(2026, 6, 26, 10, 0);
    private static final LocalDateTime STARTED_AT = ENDED_AT.minusMinutes(5);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    @Autowired
    private CallAnalysisRequestFacade callAnalysisRequestFacade;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private CallRecordingRepository callRecordingRepository;

    @Autowired
    private CallTranscriptRepository callTranscriptRepository;

    @Autowired
    private CallAnalysisRepository callAnalysisRepository;

    @Autowired
    private FakeCallAnalysisStarter fakeCallAnalysisStarter;

    @Autowired
    private AnalysisQuotaRepository analysisQuotaRepository;

    @Autowired
    private FixedDateTimeProvider fixedDateTimeProvider;

    @BeforeEach
    void resetFake() {
        fakeCallAnalysisStarter.reset();
        fixedDateTimeProvider.setFixedTime(TODAY.atStartOfDay());
    }

    @TestConfiguration
    static class FakeStarterConfig {

        @Bean
        FakeCallAnalysisStarter fakeCallAnalysisStarter() {
            return new FakeCallAnalysisStarter();
        }

        @Bean
        CallAnalysisStarter callAnalysisStarter(final FakeCallAnalysisStarter fake) {
            return fake;
        }

        @Bean
        @Primary
        FixedDateTimeProvider dateTimeProvider() {
            return new FixedDateTimeProvider(TODAY.atStartOfDay());
        }
    }

    @Test
    @DisplayName("호출자 본인 행은 requested=true, 짝꿍 행은 requested=false 로 생성되고 starter는 1회 호출된다")
    void request_whenReady_marksSelfRequestedAndPeerNotRequested() {
        // given
        final Call call = saveEndedCall(1L, 2L);
        saveRecording(call.getId(), 1L, "call-recordings/%d/1/abc".formatted(call.getId()));
        saveRecording(call.getId(), 2L, "call-recordings/%d/2/def".formatted(call.getId()));

        // when
        final CallAnalysisStartResponse response = callAnalysisRequestFacade.request(call.getId(), 1L);

        // then
        final Long selfAnalysisId = callAnalysisRepository.findByCallIdAndUserId(call.getId(), 1L)
                .orElseThrow().getId();
        assertThat(response.analysisId()).isEqualTo(selfAnalysisId);
        assertThat(callTranscriptRepository.findByCallId(call.getId())).isPresent();
        assertThat(callAnalysisRepository.findByCallIdAndUserId(call.getId(), 1L))
                .hasValueSatisfying(a -> {
                    assertThat(a.getStatus()).isEqualTo(CallAnalysisStatus.PROCESSING);
                    assertThat(a.isRequested()).isTrue();
                });
        assertThat(callAnalysisRepository.findByCallIdAndUserId(call.getId(), 2L))
                .hasValueSatisfying(a -> {
                    assertThat(a.getStatus()).isEqualTo(CallAnalysisStatus.PROCESSING);
                    assertThat(a.isRequested()).isFalse();
                });
        assertThat(fakeCallAnalysisStarter.invocations()).hasSize(1);
        assertThat(fakeCallAnalysisStarter.invocations().get(0).callId()).isEqualTo(call.getId());
        assertThat(fakeCallAnalysisStarter.invocations().get(0).recordings())
                .extracting("userId").containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("ClientB가 늦게 POST하면 본인 행만 requested=true 로 flip 되고 starter 재호출 없음")
    void request_whenPeerLaterPosts_flipsPeerRequestedAndDoesNotReinvoke() {
        // given: userA가 먼저 트리거
        final Call call = saveEndedCall(1L, 2L);
        saveRecording(call.getId(), 1L, "call-recordings/%d/1/abc".formatted(call.getId()));
        saveRecording(call.getId(), 2L, "call-recordings/%d/2/def".formatted(call.getId()));
        callAnalysisRequestFacade.request(call.getId(), 1L);
        fakeCallAnalysisStarter.reset();

        // when: userB가 동일 통화에 대해 POST
        final CallAnalysisStartResponse second = callAnalysisRequestFacade.request(call.getId(), 2L);

        // then: userB 본인의 analysisId 반환, requested 양쪽 다 true, starter 재호출 없음
        final Long userBAnalysisId = callAnalysisRepository.findByCallIdAndUserId(call.getId(), 2L)
                .orElseThrow().getId();
        assertThat(second.analysisId()).isEqualTo(userBAnalysisId);
        assertThat(callAnalysisRepository.findByCallIdAndUserId(call.getId(), 1L).orElseThrow().isRequested()).isTrue();
        assertThat(callAnalysisRepository.findByCallIdAndUserId(call.getId(), 2L).orElseThrow().isRequested()).isTrue();
        assertThat(fakeCallAnalysisStarter.invocations()).isEmpty();
    }

    @Test
    @DisplayName("녹음이 부족하면 CallRecordingsNotReadyException을 던진다")
    void request_whenRecordingsMissing_throws() {
        // given
        final Call call = saveEndedCall(1L, 2L);

        // when & then
        assertThatThrownBy(() -> callAnalysisRequestFacade.request(call.getId(), 1L))
                .isInstanceOf(CallRecordingsNotReadyException.class);
        assertThat(fakeCallAnalysisStarter.invocations()).isEmpty();
    }

    @Test
    @DisplayName("통화 참여자가 아니면 CallParticipantMismatchException을 던진다")
    void request_whenNotParticipant_throws() {
        // given
        final Call call = saveEndedCall(1L, 2L);

        // when & then
        assertThatThrownBy(() -> callAnalysisRequestFacade.request(call.getId(), 99L))
                .isInstanceOf(CallParticipantMismatchException.class);
        assertThat(fakeCallAnalysisStarter.invocations()).isEmpty();
    }

    @Test
    @DisplayName("녹음 보관 기간(30일)이 지난 통화는 CallRecordingExpiredException으로 거절하고 starter 호출·차감이 없다")
    void request_whenExpired_throwsAndDoesNotConsume() {
        // given: 31일 전 종료된 통화 + 녹음 2개
        final LocalDateTime expiredEndedAt = TODAY.atStartOfDay().minusDays(31);
        final Call call = callRepository.save(
                Call.start(1L, 2L, UUID.randomUUID(), expiredEndedAt.minusMinutes(5)));
        call.end(expiredEndedAt);
        callRepository.save(call);
        saveRecording(call.getId(), 1L, "call-recordings/%d/1/a".formatted(call.getId()));
        saveRecording(call.getId(), 2L, "call-recordings/%d/2/b".formatted(call.getId()));

        // when & then
        assertThatThrownBy(() -> callAnalysisRequestFacade.request(call.getId(), 1L))
                .isInstanceOf(CallRecordingExpiredException.class);
        assertThat(fakeCallAnalysisStarter.invocations()).isEmpty();
        assertThat(analysisQuotaRepository.findByUserId(1L)).isEmpty();
    }

    @Nested
    @DisplayName("쿼터 차감: 무료 → 유료, 소진 시 403, 재열람 무료")
    class QuotaDeduction {

        @Test
        @DisplayName("최초 요청 시 호출자의 무료 티켓을 차감한다")
        void request_whenFirstTime_consumesFreeTicket() {
            // given
            final Call call = saveReadyCall(1L, 2L);

            // when
            callAnalysisRequestFacade.request(call.getId(), 1L);

            // then
            assertThat(analysisQuotaRepository.findByUserId(1L).orElseThrow().freeRemainingOn(TODAY)).isZero();
        }

        @Test
        @DisplayName("무료를 모두 쓴 뒤 다른 통화를 새로 요청하면 403을 던지고 전체 롤백된다")
        void request_whenExhausted_throwsAndRollsBack() {
            // given: callA로 무료 소진
            final Call callA = saveReadyCall(1L, 2L);
            callAnalysisRequestFacade.request(callA.getId(), 1L);
            fakeCallAnalysisStarter.reset();
            final Call callB = saveReadyCall(1L, 3L);

            // when & then
            assertThatThrownBy(() -> callAnalysisRequestFacade.request(callB.getId(), 1L))
                    .isInstanceOf(AnalysisQuotaExhaustedException.class);
            assertThat(callTranscriptRepository.findByCallId(callB.getId())).isEmpty();
            assertThat(callAnalysisRepository.findByCallIdAndUserId(callB.getId(), 1L)).isEmpty();
            assertThat(fakeCallAnalysisStarter.invocations()).isEmpty();
        }

        @Test
        @DisplayName("무료 소진 후 유료 티켓이 있으면 유료에서 차감한다")
        void request_whenFreeExhaustedButPaidAvailable_consumesPaid() {
            // given: 유료 3장 충전 + callA로 무료 소진
            chargeQuota(1L, 3);
            final Call callA = saveReadyCall(1L, 2L);
            callAnalysisRequestFacade.request(callA.getId(), 1L);
            final Call callB = saveReadyCall(1L, 3L);

            // when
            callAnalysisRequestFacade.request(callB.getId(), 1L);

            // then
            final AnalysisQuota quota = analysisQuotaRepository.findByUserId(1L).orElseThrow();
            assertThat(quota.freeRemainingOn(TODAY)).isZero();
            assertThat(quota.paidRemaining()).isEqualTo(2);
        }

        @Test
        @DisplayName("이미 요청한 통화를 재요청하면 소진 상태여도 차감 없이 허용된다")
        void request_whenReopeningRequestedCall_doesNotConsume() {
            // given: callA로 무료 소진
            final Call callA = saveReadyCall(1L, 2L);
            callAnalysisRequestFacade.request(callA.getId(), 1L);

            // when: 같은 통화 재요청 (이미 requested)
            callAnalysisRequestFacade.request(callA.getId(), 1L);

            // then: 추가 차감 없음, 예외도 없음
            final AnalysisQuota quota = analysisQuotaRepository.findByUserId(1L).orElseThrow();
            assertThat(quota.freeRemainingOn(TODAY)).isZero();
            assertThat(quota.paidRemaining()).isZero();
        }

        @Test
        @DisplayName("두 참여자는 각자의 무료 티켓을 차감한다")
        void request_whenBothParticipants_consumeSeparately() {
            // given
            final Call call = saveReadyCall(1L, 2L);

            // when
            callAnalysisRequestFacade.request(call.getId(), 1L);
            callAnalysisRequestFacade.request(call.getId(), 2L);

            // then
            assertThat(analysisQuotaRepository.findByUserId(1L).orElseThrow().freeRemainingOn(TODAY)).isZero();
            assertThat(analysisQuotaRepository.findByUserId(2L).orElseThrow().freeRemainingOn(TODAY)).isZero();
        }

        @Test
        @DisplayName("자정이 지나면 무료가 리필되어 다시 분석할 수 있다")
        void request_whenNewDay_freeRefilled() {
            // given: 오늘 무료 소진
            setDate(TODAY);
            final Call callA = saveReadyCall(1L, 2L);
            callAnalysisRequestFacade.request(callA.getId(), 1L);

            // when: 다음 날 다른 통화 요청
            setDate(TOMORROW);
            final Call callB = saveReadyCall(1L, 3L);
            callAnalysisRequestFacade.request(callB.getId(), 1L);

            // then
            final AnalysisQuota quota = analysisQuotaRepository.findByUserId(1L).orElseThrow();
            assertThat(quota.freeResetDate()).isEqualTo(TOMORROW);
            assertThat(quota.freeRemainingOn(TOMORROW)).isZero();
        }
    }

    private Call saveReadyCall(final Long userA, final Long userB) {
        final Call call = saveEndedCall(userA, userB);
        saveRecording(call.getId(), userA, "call-recordings/%d/%d/a".formatted(call.getId(), userA));
        saveRecording(call.getId(), userB, "call-recordings/%d/%d/b".formatted(call.getId(), userB));
        return call;
    }

    private void chargeQuota(final Long userId, final int count) {
        final AnalysisQuota quota = AnalysisQuota.initial(userId);
        quota.charge(count);
        analysisQuotaRepository.save(quota);
    }

    private void setDate(final LocalDate date) {
        fixedDateTimeProvider.setFixedTime(date.atStartOfDay());
    }

    private Call saveEndedCall(final Long userA, final Long userB) {
        final Call call = callRepository.save(Call.start(userA, userB, UUID.randomUUID(), STARTED_AT));
        call.end(ENDED_AT);
        return callRepository.save(call);
    }

    private void saveRecording(final Long callId, final Long userId, final String key) {
        callRecordingRepository.save(CallRecording.upload(callId, userId, key, "audio/m4a"));
    }
}
