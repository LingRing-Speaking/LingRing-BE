package com.lingring.domain.review.domain.analysis.vo;

import java.util.Objects;

public record AnalysisResult(Mistakes mistakes, Positives positives) {

    public AnalysisResult {
        Objects.requireNonNull(mistakes, "mistakes must not be null");
        Objects.requireNonNull(positives, "positives must not be null");
    }

    public static AnalysisResult empty() {
        return new AnalysisResult(Mistakes.empty(), Positives.empty());
    }
}
