package com.lingring.domain.callanalysis.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.callanalysis.dao.CallRecordingRepository;
import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.callanalysis.dao.CallTranscriptRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.callanalysis.domain.CallRecording;
import com.lingring.domain.call.exception.CallParticipantMismatchException;
import com.lingring.domain.callanalysis.exception.CallRecordingsNotReadyException;
import com.lingring.domain.callanalysis.dao.CallAnalysisRepository;
import com.lingring.domain.callanalysis.domain.CallAnalysisStarter;
import com.lingring.domain.callanalysis.domain.CallAnalysisStatus;
import com.lingring.domain.callanalysis.dto.response.CallAnalysisStartResponse;
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

    private Call saveEndedCall(final Long userA, final Long userB) {
        final Call call = callRepository.save(Call.start(userA, userB, UUID.randomUUID(), STARTED_AT));
        call.end(ENDED_AT);
        return callRepository.save(call);
    }

    private void saveRecording(final Long callId, final Long userId, final String key) {
        callRecordingRepository.save(CallRecording.upload(callId, userId, key, "audio/m4a"));
    }
}
