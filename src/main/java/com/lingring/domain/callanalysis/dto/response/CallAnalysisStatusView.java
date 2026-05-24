package com.lingring.domain.callanalysis.dto.response;

import com.lingring.domain.callanalysis.domain.CallAnalysisStatus;
import com.lingring.domain.callanalysis.service.CallAnalysisSummary;

public enum CallAnalysisStatusView {

    READY,
    PROCESSING,
    COMPLETED,
    FAILED;

    public static CallAnalysisStatusView from(final CallAnalysisSummary summary) {
        if (summary == null) {
            return READY;
        }
        return fromStatus(summary.status());
    }

    private static CallAnalysisStatusView fromStatus(final CallAnalysisStatus status) {
        return CallAnalysisStatusView.valueOf(status.name());
    }
}