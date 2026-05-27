package com.lingring.domain.review.dao.dto;

import com.lingring.domain.review.domain.analysis.CallAnalysisStatus;

public interface CallAnalysisSummaryProjection {

    Long getCallId();

    Long getAnalysisId();

    CallAnalysisStatus getStatus();
}