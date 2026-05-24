package com.lingring.domain.callanalysis.service;

import com.lingring.domain.callanalysis.domain.CallAnalysisStatus;

public record CallAnalysisSummary(Long analysisId, CallAnalysisStatus status) {
}