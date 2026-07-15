package com.lingring.domain.review.domain.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.review.domain.analysis.vo.AnalysisResult;
import com.lingring.domain.review.domain.analysis.vo.FeedbackTag;
import com.lingring.domain.review.domain.analysis.vo.MistakeItem;
import com.lingring.domain.review.domain.analysis.vo.Mistakes;
import com.lingring.domain.review.domain.analysis.vo.Positives;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CallAnalysisTest {

    private static MistakeItem mistake(final String improved) {
        return new MistakeItem(FeedbackTag.GRAMMAR, "wrong", improved, "이유", "뜻");
    }

    private static CallAnalysis completedWith(final List<MistakeItem> mistakes) {
        final CallAnalysis analysis = CallAnalysis.processing(1L, 1L);
        analysis.complete(
                new AnalysisResult(new Mistakes(mistakes), Positives.empty()),
                "test-model"
        );
        return analysis;
    }

    @Nested
    @DisplayName("findMistake: 결과 내 인덱스로 mistake 조회")
    class FindMistake {

        @Test
        @DisplayName("유효한 인덱스면 해당 mistake를 반환한다")
        void findMistake_whenValidIndex_returnsItem() {
            // given
            final CallAnalysis analysis = completedWith(List.of(
                    mistake("first"), mistake("second")
            ));

            // when
            final Optional<MistakeItem> found = analysis.findMistake(1);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().improved()).isEqualTo("second");
        }

        @Test
        @DisplayName("인덱스가 범위를 벗어나면 빈 Optional을 반환한다")
        void findMistake_whenIndexOutOfRange_returnsEmpty() {
            // given
            final CallAnalysis analysis = completedWith(List.of(mistake("only")));

            // when & then
            assertThat(analysis.findMistake(1)).isEmpty();
            assertThat(analysis.findMistake(-1)).isEmpty();
        }

        @Test
        @DisplayName("아직 완료되지 않은(PROCESSING) 분석이면 빈 Optional을 반환한다")
        void findMistake_whenNotCompleted_returnsEmpty() {
            // given
            final CallAnalysis processing = CallAnalysis.processing(1L, 1L);

            // when & then
            assertThat(processing.findMistake(0)).isEmpty();
        }
    }
}
