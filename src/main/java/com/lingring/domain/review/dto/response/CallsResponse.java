package com.lingring.domain.review.dto.response;

import com.lingring.domain.call.dao.dto.CallSummaryProjection;
import com.lingring.domain.review.service.CallAnalysisSummary;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Slice;

public record CallsResponse(
        List<CallSummaryResponse> items,
        boolean hasNext
) {

    public static CallsResponse from(
            final Slice<CallSummaryProjection> slice,
            final Map<Long, CallAnalysisSummary> analysisSummaryByCallId
    ) {
        final List<CallSummaryResponse> items = slice.getContent().stream()
                .map(p -> CallSummaryResponse.from(p, analysisSummaryByCallId.get(p.getId())))
                .toList();
        return new CallsResponse(items, slice.hasNext());
    }
}