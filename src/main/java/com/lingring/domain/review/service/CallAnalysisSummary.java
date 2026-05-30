package com.lingring.domain.review.service;

import com.lingring.domain.review.domain.analysis.CallAnalysisStatus;

public record CallAnalysisSummary(Long analysisId, CallAnalysisStatus status) {
}