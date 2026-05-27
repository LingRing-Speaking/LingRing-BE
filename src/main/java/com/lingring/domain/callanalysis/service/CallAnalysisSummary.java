package com.lingring.domain.callanalysis.service;

import com.lingring.domain.callanalysis.domain.analysis.CallAnalysisStatus;

public record CallAnalysisSummary(Long analysisId, CallAnalysisStatus status) {
}