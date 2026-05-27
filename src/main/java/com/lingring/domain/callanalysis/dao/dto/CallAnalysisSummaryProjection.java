package com.lingring.domain.callanalysis.dao.dto;

import com.lingring.domain.callanalysis.domain.analysis.CallAnalysisStatus;

public interface CallAnalysisSummaryProjection {

    Long getCallId();

    Long getAnalysisId();

    CallAnalysisStatus getStatus();
}