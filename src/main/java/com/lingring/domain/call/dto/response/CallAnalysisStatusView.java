package com.lingring.domain.call.dto.response;

import com.lingring.domain.call.domain.port.AnalysisSummaryView;

public enum CallAnalysisStatusView {

    READY,
    PROCESSING,
    COMPLETED,
    FAILED;

    public static CallAnalysisStatusView from(final AnalysisSummaryView summary) {
        if (summary == null) {
            return READY;
        }
        return CallAnalysisStatusView.valueOf(summary.status());
    }
}