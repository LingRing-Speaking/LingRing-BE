package com.lingring.domain.review.service;

import com.lingring.domain.review.domain.analysis.CallAnalysis;

public record RequestResult(CallAnalysis analysis, boolean freshlyRequested) {
}
