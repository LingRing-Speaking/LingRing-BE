package com.lingring.domain.review.dto.response;

import com.lingring.domain.review.domain.analysis.CallAnalysis;
import com.lingring.domain.review.domain.analysis.CallAnalysisStatus;
import com.lingring.domain.review.domain.analysis.vo.AnalysisResult;
import com.lingring.domain.review.domain.analysis.vo.MistakeItem;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public record CallAnalysisResponse(
        Long callId,
        Long userId,
        CallAnalysisStatus status,
        String modelIdentifier,
        List<MistakeItemResponse> mistakes,
        List<PositiveItemResponse> positives
) {

    public static CallAnalysisResponse from(
            final CallAnalysis analysis,
            final Map<Integer, Long> bookmarkIdByMistakeIndex
    ) {
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
        final List<MistakeItem> mistakeItems = result.mistakes().items();
        final List<MistakeItemResponse> mistakes = IntStream.range(0, mistakeItems.size())
                .mapToObj(index -> MistakeItemResponse.from(
                        mistakeItems.get(index), index, bookmarkIdByMistakeIndex.get(index)))
                .toList();
        return new CallAnalysisResponse(
                analysis.getCallId(),
                analysis.getUserId(),
                analysis.getStatus(),
                analysis.getModelIdentifier(),
                mistakes,
                result.positives().items().stream().map(PositiveItemResponse::from).toList()
        );
    }
}
