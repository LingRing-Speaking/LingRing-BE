package com.lingring.domain.call.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.call.domain.port.AnalysisSummaryView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CallAnalysisStatusViewTest {

    @Nested
    @DisplayName("분석 요청 전 (summary == null): 녹음 준비 여부로 결정")
    class WhenNotRequested {

        @Test
        @DisplayName("녹음이 아직 준비되지 않았으면 WAITING_RECORDINGS (버튼 비활성)")
        void from_whenNotRequestedAndRecordingsNotReady_returnsWaitingRecordings() {
            // when & then
            assertThat(CallAnalysisStatusView.from(null, false))
                    .isEqualTo(CallAnalysisStatusView.WAITING_RECORDINGS);
        }

        @Test
        @DisplayName("녹음이 모두 준비됐으면 READY (분석 가능)")
        void from_whenNotRequestedAndRecordingsReady_returnsReady() {
            // when & then
            assertThat(CallAnalysisStatusView.from(null, true))
                    .isEqualTo(CallAnalysisStatusView.READY);
        }
    }

    @Nested
    @DisplayName("분석 요청 후 (summary != null): 녹음 준비 여부와 무관하게 분석 상태 그대로")
    class WhenRequested {

        @Test
        @DisplayName("PROCESSING 분석은 녹음 준비 여부와 무관하게 PROCESSING")
        void from_whenProcessing_returnsProcessing() {
            // given
            final AnalysisSummaryView summary = new AnalysisSummaryView(100L, "PROCESSING");

            // when & then
            assertThat(CallAnalysisStatusView.from(summary, false)).isEqualTo(CallAnalysisStatusView.PROCESSING);
        }

        @Test
        @DisplayName("COMPLETED 분석은 COMPLETED")
        void from_whenCompleted_returnsCompleted() {
            // given
            final AnalysisSummaryView summary = new AnalysisSummaryView(100L, "COMPLETED");

            // when & then
            assertThat(CallAnalysisStatusView.from(summary, true)).isEqualTo(CallAnalysisStatusView.COMPLETED);
        }
    }
}
