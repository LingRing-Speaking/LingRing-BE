package com.lingring.domain.review.facade;

import com.lingring.domain.call.dao.dto.CallSummaryProjection;
import com.lingring.domain.review.dto.response.CallsResponse;
import com.lingring.domain.call.service.CallService;
import com.lingring.domain.review.service.CallAnalysisService;
import com.lingring.domain.review.service.CallAnalysisSummary;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CallHistoryFacade {

    private final CallService callService;
    private final CallAnalysisService callAnalysisService;

    @Transactional(readOnly = true)
    public CallsResponse getCallsByUserId(final Long userId, final int page, final int size) {
        final Slice<CallSummaryProjection> slice = callService.findEndedSummariesByUserId(userId, page, size);
        final List<Long> callIds = slice.getContent().stream()
                .map(CallSummaryProjection::getId)
                .toList();
        final Map<Long, CallAnalysisSummary> analysisSummaryByCallId =
                callAnalysisService.findRequestedAnalysisSummariesByCallIds(userId, callIds);
        return CallsResponse.from(slice, analysisSummaryByCallId);
    }
}