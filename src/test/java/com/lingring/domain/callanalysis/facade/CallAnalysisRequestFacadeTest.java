package com.lingring.domain.callanalysis.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.call.dao.CallRecordingRepository;
import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.dao.CallTranscriptRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.domain.CallRecording;
import com.lingring.domain.call.dto.response.CallTranscriptStartResponse;
import com.lingring.domain.call.exception.CallParticipantMismatchException;
import com.lingring.domain.call.exception.CallRecordingsNotReadyException;
import com.lingring.domain.callanalysis.dao.CallAnalysisRepository;
import com.lingring.domain.callanalysis.domain.CallAnalysisStarter;
import com.lingring.domain.callanalysis.domain.CallAnalysisStatus;
import com.lingring.domain.callanalysis.support.FakeCallAnalysisStarter;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(CallAnalysisRequestFacadeTest.FakeStarterConfig.class)
class CallAnalysisRequestFacadeTest extends ServiceIntegrationHelper {

    private static final LocalDateTime ENDED_AT = LocalDateTime.of(2026, 5, 20, 10, 0);
    private static final LocalDateTime STARTED_AT = ENDED_AT.minusMinutes(5);

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

    @BeforeEach
    void resetFake() {
        fakeCallAnalysisStarter.reset();
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
    }

    @Test
    @DisplayName("두 녹음이 준비되어 있으면 transcript와 분석 행 2개(PROCESSING)를 생성하고 starter를 1회 호출한다")
    void request_whenReady_createsTranscriptAndAnalysesAndInvokesStarter() {
        // given
        final Call call = saveEndedCall(1L, 2L);
        saveRecording(call.getId(), 1L, "call-recordings/%d/1/abc".formatted(call.getId()));
        saveRecording(call.getId(), 2L, "call-recordings/%d/2/def".formatted(call.getId()));

        // when
        final CallTranscriptStartResponse response = callAnalysisRequestFacade.request(call.getId(), 1L);

        // then
        assertThat(response.transcriptId()).isNotNull();
        assertThat(callTranscriptRepository.findByCallId(call.getId())).isPresent();
        assertThat(callAnalysisRepository.findByCallIdAndUserId(call.getId(), 1L))
                .hasValueSatisfying(a -> assertThat(a.getStatus()).isEqualTo(CallAnalysisStatus.PROCESSING));
        assertThat(callAnalysisRepository.findByCallIdAndUserId(call.getId(), 2L))
                .hasValueSatisfying(a -> assertThat(a.getStatus()).isEqualTo(CallAnalysisStatus.PROCESSING));
        assertThat(fakeCallAnalysisStarter.invocations()).hasSize(1);
        assertThat(fakeCallAnalysisStarter.invocations().get(0).callId()).isEqualTo(call.getId());
        assertThat(fakeCallAnalysisStarter.invocations().get(0).recordings())
                .extracting("userId").containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("이미 transcript가 존재하면 starter를 재호출하지 않고 기존 응답을 반환한다 (멱등)")
    void request_whenTranscriptExists_isIdempotent() {
        // given
        final Call call = saveEndedCall(1L, 2L);
        saveRecording(call.getId(), 1L, "call-recordings/%d/1/abc".formatted(call.getId()));
        saveRecording(call.getId(), 2L, "call-recordings/%d/2/def".formatted(call.getId()));
        final CallTranscriptStartResponse first = callAnalysisRequestFacade.request(call.getId(), 1L);
        fakeCallAnalysisStarter.reset();

        // when
        final CallTranscriptStartResponse second = callAnalysisRequestFacade.request(call.getId(), 2L);

        // then
        assertThat(second.transcriptId()).isEqualTo(first.transcriptId());
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

    private Call saveEndedCall(final Long userA, final Long userB) {
        final Call call = callRepository.save(Call.start(userA, userB, UUID.randomUUID(), STARTED_AT));
        call.end(ENDED_AT);
        return callRepository.save(call);
    }

    private void saveRecording(final Long callId, final Long userId, final String key) {
        callRecordingRepository.save(CallRecording.upload(callId, userId, key, "audio/m4a"));
    }
}
