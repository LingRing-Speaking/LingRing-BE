package com.lingring.domain.call.dto.response;

import com.lingring.domain.call.dao.dto.CallSummaryProjection;
import java.util.List;
import org.springframework.data.domain.Slice;

public record CallsResponse(
        List<CallSummaryResponse> items,
        boolean hasNext
) {

    public static CallsResponse from(final Slice<CallSummaryProjection> slice) {
        final List<CallSummaryResponse> items = slice.getContent().stream()
                .map(CallSummaryResponse::from)
                .toList();
        return new CallsResponse(items, slice.hasNext());
    }
}
