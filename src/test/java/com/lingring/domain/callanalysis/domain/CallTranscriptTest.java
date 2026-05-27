package com.lingring.domain.callanalysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.callanalysis.domain.vo.TranscriptContent;
import com.lingring.domain.callanalysis.domain.vo.TranscriptSegment;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CallTranscriptTest {

    @Nested
    @DisplayName("create: 신규 transcript 생성 팩토리")
    class Create {

        @Test
        @DisplayName("callId를 보관하고 content는 null로 시작한다")
        void create_whenValid_createsEmptyTranscript() {
            // when
            final CallTranscript transcript = CallTranscript.create(1L);

            // then
            assertThat(transcript.getCallId()).isEqualTo(1L);
            assertThat(transcript.getContent()).isNull();
        }

        @Test
        @DisplayName("callId가 null이면 NullPointerException이 발생한다")
        void create_whenCallIdNull_throws() {
            // when & then
            assertThatThrownBy(() -> CallTranscript.create(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("complete: SQS 결과 수신 후 segments 저장")
    class Complete {

        @Test
        @DisplayName("content가 null이면 새 content로 채운다")
        void complete_whenContentEmpty_fillsContent() {
            // given
            final CallTranscript transcript = CallTranscript.create(1L);
            final TranscriptContent content = sampleContent();

            // when
            transcript.complete(content);

            // then
            assertThat(transcript.getContent()).isEqualTo(content);
        }

        @Test
        @DisplayName("이미 content가 채워져 있으면 다시 호출해도 무시된다 (멱등)")
        void complete_whenAlreadyCompleted_isIdempotent() {
            // given
            final CallTranscript transcript = CallTranscript.create(1L);
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
            final CallTranscript transcript = CallTranscript.create(1L);

            // when & then
            assertThatThrownBy(() -> transcript.complete(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    private TranscriptContent sampleContent() {
        return new TranscriptContent(List.of(
                new TranscriptSegment(101L, 0.0, 2.1, "안녕"),
                new TranscriptSegment(202L, 2.5, 4.8, "오 안녕")
        ));
    }
}
