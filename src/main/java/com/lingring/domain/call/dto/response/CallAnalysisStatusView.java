package com.lingring.domain.call.dto.response;

import com.lingring.domain.call.domain.port.AnalysisSummaryView;

public enum CallAnalysisStatusView {

    WAITING_RECORDINGS,
    READY,
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED;

    public static CallAnalysisStatusView from(
            final AnalysisSummaryView summary,
            final boolean recordingsReady,
            final boolean expired
    ) {
        final CallAnalysisStatusView base = base(summary, recordingsReady);
        if (expired && base.expirable()) {
            return EXPIRED;
        }
        return base;
    }

    public static CallAnalysisStatusView ofRequested(final String analysisStatus, final boolean expired) {
        final CallAnalysisStatusView base = valueOf(analysisStatus);
        if (expired && base.expirable()) {
            return EXPIRED;
        }
        return base;
    }

    private static CallAnalysisStatusView base(
            final AnalysisSummaryView summary,
            final boolean recordingsReady
    ) {
        if (summary != null) {
            return valueOf(summary.status());
        }
        if (recordingsReady) {
            return READY;
        }
        return WAITING_RECORDINGS;
    }

    /**
     * 녹음 의존 동작(분석 시작/재시도)이 가능한 상태만 만료(EXPIRED)로 전이된다.
     * COMPLETED·PROCESSING은 녹음 삭제와 무관하므로 유지한다.
     */
    private boolean expirable() {
        return this == READY || this == FAILED || this == WAITING_RECORDINGS;
    }
}
