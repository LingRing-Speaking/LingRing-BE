package com.lingring.domain.call.dto.response;

import com.lingring.domain.call.domain.port.AnalysisSummaryView;

public enum CallAnalysisStatusView {

    WAITING_RECORDINGS,
    READY,
    PROCESSING,
    COMPLETED,
    FAILED;

    public static CallAnalysisStatusView from(final AnalysisSummaryView summary, final boolean recordingsReady) {
        if (summary != null) {
            return CallAnalysisStatusView.valueOf(summary.status());
        }
        if (recordingsReady) {
            return READY;
        }
        return WAITING_RECORDINGS;
    }
}