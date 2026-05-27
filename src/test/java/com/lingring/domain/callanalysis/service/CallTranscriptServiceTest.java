package com.lingring.domain.callanalysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.callanalysis.dao.CallRecordingRepository;
import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.callanalysis.dao.CallTranscriptRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.callanalysis.domain.CallRecording;
import com.lingring.domain.callanalysis.domain.CallTranscript;
import com.lingring.domain.callanalysis.domain.vo.TranscriptContent;
import com.lingring.domain.callanalysis.domain.vo.TranscriptSegment;
import com.lingring.domain.call.exception.CallActiveException;
import com.lingring.domain.call.exception.CallNotFoundException;
import com.lingring.domain.call.exception.CallParticipantMismatchException;
import com.lingring.domain.callanalysis.exception.CallRecordingsNotReadyException;
import com.lingring.domain.callanalysis.exception.CallTranscriptNotFoundException;
import com.lingring.domain.callanalysis.service.CallTranscriptService.StartTranscriptResult;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Nested
    @DisplayName("startTranscript: transcript 생성 및 정보 반환")
    class StartTranscript {

        @Test
        @DisplayName("두 녹음이 모두 있으면 freshlyCreated=true와 신규 transcript를 반환한다")
        void startTranscript_whenBothRecordingsReady_returnsFreshlyCreated() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            saveRecording(call.getId(), 1L, "call-recordings/%d/1/abc".formatted(call.getId()));
            saveRecording(call.getId(), 2L, "call-recordings/%d/2/def".formatted(call.getId()));

            // when
            final StartTranscriptResult result = callTranscriptService.startTranscript(call.getId(), 1L);

            // then
            assertThat(result.freshlyCreated()).isTrue();
            assertThat(result.transcript().getContent()).isNull();
            assertThat(result.userAId()).isEqualTo(1L);
            assertThat(result.userBId()).isEqualTo(2L);
            assertThat(result.recordings()).extracting("userId").containsExactlyInAnyOrder(1L, 2L);
            assertThat(callTranscriptRepository.findByCallId(call.getId())).isPresent();
        }

        @Test
        @DisplayName("이미 transcript가 존재하면 freshlyCreated=false와 기존 transcript를 반환한다 (멱등)")
        void startTranscript_whenAlreadyExists_returnsExisting() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            saveRecording(call.getId(), 1L, "call-recordings/%d/1/abc".formatted(call.getId()));
            saveRecording(call.getId(), 2L, "call-recordings/%d/2/def".formatted(call.getId()));
            final StartTranscriptResult first = callTranscriptService.startTranscript(call.getId(), 1L);

            // when
            final StartTranscriptResult second = callTranscriptService.startTranscript(call.getId(), 2L);

            // then
            assertThat(second.freshlyCreated()).isFalse();
            assertThat(second.transcript().getId()).isEqualTo(first.transcript().getId());
            assertThat(second.recordings()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 callId면 CallNotFoundException")
        void startTranscript_whenCallMissing_throws() {
            // when & then
            assertThatThrownBy(() -> callTranscriptService.startTranscript(9999L, 1L))
                    .isInstanceOf(CallNotFoundException.class);
        }

        @Test
        @DisplayName("통화 참여자가 아니면 CallParticipantMismatchException")
        void startTranscript_whenNotParticipant_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);

            // when & then
            assertThatThrownBy(() -> callTranscriptService.startTranscript(call.getId(), 99L))
                    .isInstanceOf(CallParticipantMismatchException.class);
        }

        @Test
        @DisplayName("통화가 아직 활성 상태면 CallActiveException")
        void startTranscript_whenCallActive_throws() {
            // given
            final Call active = callRepository.save(Call.start(1L, 2L, UUID.randomUUID(), STARTED_AT));

            // when & then
            assertThatThrownBy(() -> callTranscriptService.startTranscript(active.getId(), 1L))
                    .isInstanceOf(CallActiveException.class);
        }

        @Test
        @DisplayName("두 녹음이 모두 없으면 CallRecordingsNotReadyException")
        void startTranscript_whenNoRecordings_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);

            // when & then
            assertThatThrownBy(() -> callTranscriptService.startTranscript(call.getId(), 1L))
                    .isInstanceOf(CallRecordingsNotReadyException.class);
        }

        @Test
        @DisplayName("한쪽 녹음만 있으면 CallRecordingsNotReadyException")
        void startTranscript_whenOnlyOneRecording_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            saveRecording(call.getId(), 1L, "call-recordings/%d/1/abc".formatted(call.getId()));

            // when & then
            assertThatThrownBy(() -> callTranscriptService.startTranscript(call.getId(), 1L))
                    .isInstanceOf(CallRecordingsNotReadyException.class);
        }
    }

    @Nested
    @DisplayName("complete: SQS 결과 수신 후 영속화")
    class Complete {

        @Test
        @DisplayName("transcript에 content가 비어있으면 segments로 채운다")
        void complete_whenEmpty_fillsContent() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            callTranscriptRepository.save(CallTranscript.create(call.getId()));
            final List<TranscriptSegment> segments = List.of(
                    new TranscriptSegment(1L, 0.0, 2.1, "안녕"),
                    new TranscriptSegment(2L, 2.5, 4.8, "오 안녕")
            );

            // when
            callTranscriptService.complete(call.getId(), segments);

            // then
            final CallTranscript saved = callTranscriptRepository.findByCallId(call.getId()).orElseThrow();
            assertThat(saved.getContent().segments()).hasSize(2);
        }

        @Test
        @DisplayName("이미 content가 채워져 있으면 다시 호출해도 덮어쓰지 않는다 (멱등)")
        void complete_whenAlreadyCompleted_isIdempotent() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            final CallTranscript transcript = CallTranscript.create(call.getId());
            final List<TranscriptSegment> initialSegments = List.of(
                    new TranscriptSegment(1L, 0.0, 1.0, "initial")
            );
            transcript.complete(new TranscriptContent(initialSegments));
            callTranscriptRepository.save(transcript);

            // when
            callTranscriptService.complete(call.getId(), List.of(
                    new TranscriptSegment(1L, 0.0, 1.0, "overwritten")
            ));

            // then
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
