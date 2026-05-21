package com.lingring.domain.call.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.call.dao.CallRecordingRepository;
import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.dao.CallTranscriptRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.domain.CallRecording;
import com.lingring.domain.call.domain.CallTranscript;
import com.lingring.domain.call.domain.CallTranscriptStatus;
import com.lingring.domain.call.domain.TranscriptionStarter;
import com.lingring.domain.call.domain.vo.TranscriptContent;
import com.lingring.domain.call.domain.vo.TranscriptSegment;
import com.lingring.domain.call.dto.response.CallTranscriptResponse;
import com.lingring.domain.call.dto.response.CallTranscriptStartResponse;
import com.lingring.domain.call.exception.CallActiveException;
import com.lingring.domain.call.exception.CallNotFoundException;
import com.lingring.domain.call.exception.CallParticipantMismatchException;
import com.lingring.domain.call.exception.CallRecordingsNotReadyException;
import com.lingring.domain.call.exception.CallTranscriptNotFoundException;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(CallTranscriptServiceTest.FakeTranscriptionStarterConfig.class)
class CallTranscriptServiceTest extends ServiceIntegrationHelper {

    private static final LocalDateTime ENDED_AT = LocalDateTime.of(2026, 5, 20, 10, 0);
    private static final LocalDateTime STARTED_AT = ENDED_AT.minusMinutes(5);

    @Autowired
    private CallTranscriptService callTranscriptService;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private CallRecordingRepository callRecordingRepository;

    @Autowired
    private CallTranscriptRepository callTranscriptRepository;

    @Autowired
    private FakeTranscriptionStarter fakeTranscriptionStarter;

    @BeforeEach
    void clear() {
        fakeTranscriptionStarter.reset();
    }

    @TestConfiguration
    static class FakeTranscriptionStarterConfig {

        @Bean
        FakeTranscriptionStarter fakeTranscriptionStarter() {
            return new FakeTranscriptionStarter();
        }

        @Bean
        TranscriptionStarter transcriptionStarter(final FakeTranscriptionStarter fake) {
            return fake;
        }
    }

    @Nested
    @DisplayName("requestAnalysis: 분석 트리거")
    class RequestAnalysis {

        @Test
        @DisplayName("두 녹음이 모두 있으면 PROCESSING transcript를 생성하고 Lambda를 호출한다")
        void requestAnalysis_whenBothRecordingsReady_createsProcessingAndInvokes() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            saveRecording(call.getId(), 1L, "call-recordings/%d/1/abc".formatted(call.getId()));
            saveRecording(call.getId(), 2L, "call-recordings/%d/2/def".formatted(call.getId()));

            // when
            final CallTranscriptStartResponse response =
                    callTranscriptService.requestAnalysis(call.getId(), 1L);

            // then
            assertThat(response.status()).isEqualTo(CallTranscriptStatus.PROCESSING);
            assertThat(callTranscriptRepository.findByCallId(call.getId())).isPresent();
            assertThat(fakeTranscriptionStarter.invocations()).hasSize(1);
            assertThat(fakeTranscriptionStarter.invocations().get(0).callId()).isEqualTo(call.getId());
            assertThat(fakeTranscriptionStarter.invocations().get(0).recordings())
                    .extracting("userId")
                    .containsExactlyInAnyOrder(1L, 2L);
        }

        @Test
        @DisplayName("이미 PROCESSING 상태이면 기존 transcript 응답을 반환하고 Lambda는 재호출하지 않는다 (멱등)")
        void requestAnalysis_whenAlreadyProcessing_returnsExistingAndDoesNotReinvoke() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            saveRecording(call.getId(), 1L, "call-recordings/%d/1/abc".formatted(call.getId()));
            saveRecording(call.getId(), 2L, "call-recordings/%d/2/def".formatted(call.getId()));
            final CallTranscriptStartResponse first = callTranscriptService.requestAnalysis(call.getId(), 1L);

            // when
            final CallTranscriptStartResponse second = callTranscriptService.requestAnalysis(call.getId(), 2L);

            // then
            assertThat(second.transcriptId()).isEqualTo(first.transcriptId());
            assertThat(second.status()).isEqualTo(CallTranscriptStatus.PROCESSING);
            assertThat(fakeTranscriptionStarter.invocations()).hasSize(1);
        }

        @Test
        @DisplayName("이미 COMPLETED 상태이면 기존 transcript 응답을 반환하고 Lambda는 호출하지 않는다")
        void requestAnalysis_whenAlreadyCompleted_returnsExisting() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            saveRecording(call.getId(), 1L, "call-recordings/%d/1/abc".formatted(call.getId()));
            saveRecording(call.getId(), 2L, "call-recordings/%d/2/def".formatted(call.getId()));
            final CallTranscript completed = CallTranscript.startProcessing(call.getId());
            completed.complete(new TranscriptContent(List.of(
                    new TranscriptSegment(1L, 0.0, 1.0, "hello")
            )));
            callTranscriptRepository.save(completed);

            // when
            final CallTranscriptStartResponse response = callTranscriptService.requestAnalysis(call.getId(), 1L);

            // then
            assertThat(response.status()).isEqualTo(CallTranscriptStatus.COMPLETED);
            assertThat(fakeTranscriptionStarter.invocations()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 callId면 CallNotFoundException")
        void requestAnalysis_whenCallMissing_throws() {
            // when & then
            assertThatThrownBy(() -> callTranscriptService.requestAnalysis(9999L, 1L))
                    .isInstanceOf(CallNotFoundException.class);
        }

        @Test
        @DisplayName("통화 참여자가 아니면 CallParticipantMismatchException")
        void requestAnalysis_whenNotParticipant_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);

            // when & then
            assertThatThrownBy(() -> callTranscriptService.requestAnalysis(call.getId(), 99L))
                    .isInstanceOf(CallParticipantMismatchException.class);
        }

        @Test
        @DisplayName("통화가 아직 활성 상태면 CallActiveException")
        void requestAnalysis_whenCallActive_throws() {
            // given
            final Call active = callRepository.save(Call.start(1L, 2L, UUID.randomUUID(), STARTED_AT));

            // when & then
            assertThatThrownBy(() -> callTranscriptService.requestAnalysis(active.getId(), 1L))
                    .isInstanceOf(CallActiveException.class);
        }

        @Test
        @DisplayName("두 녹음이 모두 없으면 CallRecordingsNotReadyException")
        void requestAnalysis_whenNoRecordings_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);

            // when & then
            assertThatThrownBy(() -> callTranscriptService.requestAnalysis(call.getId(), 1L))
                    .isInstanceOf(CallRecordingsNotReadyException.class);
        }

        @Test
        @DisplayName("한쪽 녹음만 있으면 CallRecordingsNotReadyException")
        void requestAnalysis_whenOnlyOneRecording_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            saveRecording(call.getId(), 1L, "call-recordings/%d/1/abc".formatted(call.getId()));

            // when & then
            assertThatThrownBy(() -> callTranscriptService.requestAnalysis(call.getId(), 1L))
                    .isInstanceOf(CallRecordingsNotReadyException.class);
        }
    }

    @Nested
    @DisplayName("getTranscript: 조회")
    class GetTranscript {

        @Test
        @DisplayName("PROCESSING 상태에서는 status만 반환하고 segments는 null이다")
        void getTranscript_whenProcessing_returnsStatusOnly() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            callTranscriptRepository.save(CallTranscript.startProcessing(call.getId()));

            // when
            final CallTranscriptResponse response = callTranscriptService.getTranscript(call.getId(), 1L);

            // then
            assertThat(response.status()).isEqualTo(CallTranscriptStatus.PROCESSING);
            assertThat(response.segments()).isNull();
        }

        @Test
        @DisplayName("COMPLETED 상태에서는 segments를 포함하여 반환한다")
        void getTranscript_whenCompleted_returnsSegments() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            final CallTranscript transcript = CallTranscript.startProcessing(call.getId());
            transcript.complete(new TranscriptContent(List.of(
                    new TranscriptSegment(1L, 0.0, 2.1, "안녕"),
                    new TranscriptSegment(2L, 2.5, 4.8, "오 안녕")
            )));
            callTranscriptRepository.save(transcript);

            // when
            final CallTranscriptResponse response = callTranscriptService.getTranscript(call.getId(), 1L);

            // then
            assertThat(response.status()).isEqualTo(CallTranscriptStatus.COMPLETED);
            assertThat(response.segments()).hasSize(2);
            assertThat(response.segments().get(0).text()).isEqualTo("안녕");
        }

        @Test
        @DisplayName("transcript가 없으면 CallTranscriptNotFoundException")
        void getTranscript_whenMissing_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);

            // when & then
            assertThatThrownBy(() -> callTranscriptService.getTranscript(call.getId(), 1L))
                    .isInstanceOf(CallTranscriptNotFoundException.class);
        }

        @Test
        @DisplayName("통화 참여자가 아니면 CallParticipantMismatchException")
        void getTranscript_whenNotParticipant_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            callTranscriptRepository.save(CallTranscript.startProcessing(call.getId()));

            // when & then
            assertThatThrownBy(() -> callTranscriptService.getTranscript(call.getId(), 99L))
                    .isInstanceOf(CallParticipantMismatchException.class);
        }
    }

    @Nested
    @DisplayName("complete: SQS 결과 수신 후 영속화")
    class Complete {

        @Test
        @DisplayName("PROCESSING 상태에서 COMPLETED로 전이되고 content가 채워진다")
        void complete_whenProcessing_transitionsToCompleted() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            callTranscriptRepository.save(CallTranscript.startProcessing(call.getId()));
            final List<TranscriptSegment> segments = List.of(
                    new TranscriptSegment(1L, 0.0, 2.1, "안녕"),
                    new TranscriptSegment(2L, 2.5, 4.8, "오 안녕")
            );

            // when
            callTranscriptService.complete(call.getId(), segments);

            // then
            final CallTranscript saved = callTranscriptRepository.findByCallId(call.getId()).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(CallTranscriptStatus.COMPLETED);
            assertThat(saved.getContent().segments()).hasSize(2);
        }

        @Test
        @DisplayName("이미 COMPLETED 상태에서 다시 호출하면 무시된다 (멱등)")
        void complete_whenAlreadyCompleted_isIdempotent() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            final CallTranscript transcript = CallTranscript.startProcessing(call.getId());
            final List<TranscriptSegment> initialSegments = List.of(
                    new TranscriptSegment(1L, 0.0, 1.0, "initial")
            );
            transcript.complete(new TranscriptContent(initialSegments));
            callTranscriptRepository.save(transcript);

            // when: 다른 segments로 다시 complete 호출
            callTranscriptService.complete(call.getId(), List.of(
                    new TranscriptSegment(1L, 0.0, 1.0, "overwritten")
            ));

            // then: 기존 content가 유지된다
            final CallTranscript saved = callTranscriptRepository.findByCallId(call.getId()).orElseThrow();
            assertThat(saved.getContent().segments().get(0).text()).isEqualTo("initial");
        }

        @Test
        @DisplayName("transcript가 없으면 CallTranscriptNotFoundException")
        void complete_whenMissing_throws() {
            // when & then
            assertThatThrownBy(() -> callTranscriptService.complete(9999L, List.of()))
                    .isInstanceOf(CallTranscriptNotFoundException.class);
        }
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
