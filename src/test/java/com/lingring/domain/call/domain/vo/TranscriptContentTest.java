package com.lingring.domain.call.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TranscriptContentTest {

    @Nested
    @DisplayName("of: segments로부터 생성")
    class Of {

        @Test
        @DisplayName("segments를 그대로 보관한다")
        void of_whenValid_keepsSegments() {
            // given
            final List<TranscriptSegment> segments = List.of(
                    new TranscriptSegment(101L, 0.0, 2.1, "안녕"),
                    new TranscriptSegment(202L, 2.5, 4.8, "오 안녕")
            );

            // when
            final TranscriptContent content = new TranscriptContent(segments);

            // then
            assertThat(content.segments()).containsExactlyElementsOf(segments);
        }

        @Test
        @DisplayName("입력 리스트를 외부에서 변경해도 내부 상태에 영향 없다 (defensive copy)")
        void of_whenSourceListMutated_internalIsUnaffected() {
            // given
            final List<TranscriptSegment> source = new ArrayList<>();
            source.add(new TranscriptSegment(101L, 0.0, 2.1, "안녕"));
            final TranscriptContent content = new TranscriptContent(source);

            // when
            source.add(new TranscriptSegment(202L, 2.5, 4.8, "외부 추가"));

            // then
            assertThat(content.segments()).hasSize(1);
        }

        @Test
        @DisplayName("반환된 segments는 수정할 수 없다 (immutable)")
        void of_whenAttemptedMutation_throws() {
            // given
            final TranscriptContent content = new TranscriptContent(List.of(
                    new TranscriptSegment(101L, 0.0, 2.1, "안녕")
            ));

            // when & then
            assertThatThrownBy(() -> content.segments().add(
                    new TranscriptSegment(202L, 2.5, 4.8, "추가 시도")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("segments가 null이면 NullPointerException이 발생한다")
        void of_whenSegmentsNull_throws() {
            // when & then
            assertThatThrownBy(() -> new TranscriptContent(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
