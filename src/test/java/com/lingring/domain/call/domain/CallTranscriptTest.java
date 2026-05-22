package com.lingring.domain.call.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.call.domain.vo.TranscriptContent;
import com.lingring.domain.call.domain.vo.TranscriptSegment;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CallTranscriptTest {

    @Nested
    @DisplayName("startProcessing: 신규 transcript 생성 팩토리")
    class StartProcessing {

        @Test
        @DisplayName("callId를 보관하고 status=PROCESSING으로 시작한다")
        void startProcessing_whenValid_createsProcessingTranscript() {
            // when
            final CallTranscript transcript = CallTranscript.startProcessing(1L);

            // then
            assertThat(transcript.getCallId()).isEqualTo(1L);
            assertThat(transcript.getStatus()).isEqualTo(CallTranscriptStatus.PROCESSING);
            assertThat(transcript.getContent()).isNull();
            assertThat(transcript.isProcessing()).isTrue();
            assertThat(transcript.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("callId가 null이면 NullPointerException이 발생한다")
        void startProcessing_whenCallIdNull_throws() {
            // when & then
            assertThatThrownBy(() -> CallTranscript.startProcessing(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("complete: STT 결과 수신 후 완료 처리")
    class Complete {

        @Test
        @DisplayName("PROCESSING에서 COMPLETED로 전이되고 content가 채워진다")
        void complete_whenProcessing_transitionsToCompleted() {
            // given
            final CallTranscript transcript = CallTranscript.startProcessing(1L);
            final TranscriptContent content = sampleContent();

            // when
            transcript.complete(content);

            // then
            assertThat(transcript.isCompleted()).isTrue();
            assertThat(transcript.getContent()).isEqualTo(content);
        }

        @Test
        @DisplayName("이미 COMPLETED 상태에서 다시 호출하면 무시된다 (멱등)")
        void complete_whenAlreadyCompleted_isIdempotent() {
            // given
            final CallTranscript transcript = CallTranscript.startProcessing(1L);
            final TranscriptContent first = sampleContent();
            transcript.complete(first);
            final TranscriptContent second = new TranscriptContent(List.of());

            // when
            transcript.complete(second);

            // then
            assertThat(transcript.getContent()).isEqualTo(first);
        }

        @Test
        @DisplayName("content가 null이면 NullPointerException이 발생한다")
        void complete_whenContentNull_throws() {
            // given
            final CallTranscript transcript = CallTranscript.startProcessing(1L);

            // when & then
            assertThatThrownBy(() -> transcript.complete(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("fail: 실패 상태 전이")
    class Fail {

        @Test
        @DisplayName("PROCESSING에서 FAILED로 전이된다")
        void fail_whenProcessing_transitionsToFailed() {
            // given
            final CallTranscript transcript = CallTranscript.startProcessing(1L);

            // when
            transcript.fail();

            // then
            assertThat(transcript.getStatus()).isEqualTo(CallTranscriptStatus.FAILED);
            assertThat(transcript.isProcessing()).isFalse();
            assertThat(transcript.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("이미 COMPLETED 상태에서는 fail이 무시된다")
        void fail_whenAlreadyCompleted_isIgnored() {
            // given
            final CallTranscript transcript = CallTranscript.startProcessing(1L);
            transcript.complete(sampleContent());

            // when
            transcript.fail();

            // then
            assertThat(transcript.isCompleted()).isTrue();
        }
    }

    private TranscriptContent sampleContent() {
        return new TranscriptContent(List.of(
                new TranscriptSegment(101L, 0.0, 2.1, "안녕"),
                new TranscriptSegment(202L, 2.5, 4.8, "오 안녕")
        ));
    }
}
