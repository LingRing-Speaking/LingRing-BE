package com.lingring.domain.review.dto.response;

import com.lingring.domain.review.domain.analysis.CallAnalysis;
import com.lingring.domain.review.domain.analysis.CallAnalysisStatus;
import com.lingring.domain.review.domain.analysis.vo.AnalysisResult;
import java.util.List;

public record CallAnalysisResponse(
        Long callId,
        Long userId,
        CallAnalysisStatus status,
        String modelIdentifier,
        List<MistakeItemResponse> mistakes,
        List<PositiveItemResponse> positives
) {

    public static CallAnalysisResponse from(final CallAnalysis analysis) {
        final AnalysisResult result = analysis.getResult();
        if (result == null) {
            return new CallAnalysisResponse(
                    analysis.getCallId(),
                    analysis.getUserId(),
                    analysis.getStatus(),
                    analysis.getModelIdentifier(),
                    List.of(),
                    List.of()
            );
        }
        return new CallAnalysisResponse(
                analysis.getCallId(),
                analysis.getUserId(),
                analysis.getStatus(),
                analysis.getModelIdentifier(),
                result.mistakes().items().stream().map(MistakeItemResponse::from).toList(),
                result.positives().items().stream().map(PositiveItemResponse::from).toList()
        );
    }
}
