package com.lingring.domain.review.adapter;

import com.lingring.domain.call.domain.port.AnalysisSummaryProvider;
import com.lingring.domain.call.domain.port.AnalysisSummaryView;
import com.lingring.domain.review.service.CallAnalysisService;
import com.lingring.domain.review.service.CallAnalysisSummary;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewAnalysisSummaryProvider implements AnalysisSummaryProvider {

    private final CallAnalysisService callAnalysisService;

    @Override
    public Map<Long, AnalysisSummaryView> findByCallIds(final Long userId, final List<Long> callIds) {
        final Map<Long, CallAnalysisSummary> summaries =
                callAnalysisService.findRequestedAnalysisSummariesByCallIds(userId, callIds);
        return summaries.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new AnalysisSummaryView(e.getValue().analysisId(), e.getValue().status().name())
                ));
    }
}
