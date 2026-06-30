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
            assertThat(CallAnalysisStatusView.from(null, false, false))
                    .isEqualTo(CallAnalysisStatusView.WAITING_RECORDINGS);
        }

        @Test
        @DisplayName("녹음이 모두 준비됐으면 READY (분석 가능)")
        void from_whenNotRequestedAndRecordingsReady_returnsReady() {
            // when & then
            assertThat(CallAnalysisStatusView.from(null, true, false))
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
            assertThat(CallAnalysisStatusView.from(summary, false, false))
                    .isEqualTo(CallAnalysisStatusView.PROCESSING);
        }

        @Test
        @DisplayName("COMPLETED 분석은 COMPLETED")
        void from_whenCompleted_returnsCompleted() {
            // given
            final AnalysisSummaryView summary = new AnalysisSummaryView(100L, "COMPLETED");

            // when & then
            assertThat(CallAnalysisStatusView.from(summary, true, false))
                    .isEqualTo(CallAnalysisStatusView.COMPLETED);
        }
    }

    @Nested
    @DisplayName("녹음 만료 (expired == true): 녹음 의존 동작이 영구 불가인 상태만 EXPIRED")
    class WhenExpired {

        @Test
        @DisplayName("READY 는 만료 시 EXPIRED")
        void from_whenReadyAndExpired_returnsExpired() {
            // when & then
            assertThat(CallAnalysisStatusView.from(null, true, true))
                    .isEqualTo(CallAnalysisStatusView.EXPIRED);
        }

        @Test
        @DisplayName("WAITING_RECORDINGS 는 만료 시 EXPIRED")
        void from_whenWaitingAndExpired_returnsExpired() {
            // when & then
            assertThat(CallAnalysisStatusView.from(null, false, true))
                    .isEqualTo(CallAnalysisStatusView.EXPIRED);
        }

        @Test
        @DisplayName("FAILED 는 만료 시 EXPIRED (재분석 불가)")
        void from_whenFailedAndExpired_returnsExpired() {
            // given
            final AnalysisSummaryView summary = new AnalysisSummaryView(100L, "FAILED");

            // when & then
            assertThat(CallAnalysisStatusView.from(summary, false, true))
                    .isEqualTo(CallAnalysisStatusView.EXPIRED);
        }

        @Test
        @DisplayName("COMPLETED 는 만료여도 유지 (분석 결과는 녹음 삭제와 무관)")
        void from_whenCompletedAndExpired_staysCompleted() {
            // given
            final AnalysisSummaryView summary = new AnalysisSummaryView(100L, "COMPLETED");

            // when & then
            assertThat(CallAnalysisStatusView.from(summary, false, true))
                    .isEqualTo(CallAnalysisStatusView.COMPLETED);
        }

        @Test
        @DisplayName("PROCESSING 은 만료여도 유지 (진행 중)")
        void from_whenProcessingAndExpired_staysProcessing() {
            // given
            final AnalysisSummaryView summary = new AnalysisSummaryView(100L, "PROCESSING");

            // when & then
            assertThat(CallAnalysisStatusView.from(summary, false, true))
                    .isEqualTo(CallAnalysisStatusView.PROCESSING);
        }
    }

    @Nested
    @DisplayName("ofRequested: 요청된 분석의 DB 상태 + 만료 여부로 결정 (status 엔드포인트)")
    class OfRequested {

        @Test
        @DisplayName("FAILED + 만료 → EXPIRED")
        void ofRequested_whenFailedAndExpired_returnsExpired() {
            assertThat(CallAnalysisStatusView.ofRequested("FAILED", true))
                    .isEqualTo(CallAnalysisStatusView.EXPIRED);
        }

        @Test
        @DisplayName("FAILED + 미만료 → FAILED")
        void ofRequested_whenFailedAndNotExpired_returnsFailed() {
            assertThat(CallAnalysisStatusView.ofRequested("FAILED", false))
                    .isEqualTo(CallAnalysisStatusView.FAILED);
        }

        @Test
        @DisplayName("COMPLETED + 만료 → COMPLETED 유지")
        void ofRequested_whenCompletedAndExpired_staysCompleted() {
            assertThat(CallAnalysisStatusView.ofRequested("COMPLETED", true))
                    .isEqualTo(CallAnalysisStatusView.COMPLETED);
        }

        @Test
        @DisplayName("PROCESSING + 만료 → PROCESSING 유지")
        void ofRequested_whenProcessingAndExpired_staysProcessing() {
            assertThat(CallAnalysisStatusView.ofRequested("PROCESSING", true))
                    .isEqualTo(CallAnalysisStatusView.PROCESSING);
        }
    }
}
