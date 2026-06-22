package com.lingring.domain.call.service;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.dao.dto.CallSummaryProjection;
import com.lingring.domain.call.domain.port.AnalysisSummaryProvider;
import com.lingring.domain.call.domain.port.AnalysisSummaryView;
import com.lingring.domain.call.dto.response.CallsResponse;
import com.lingring.global.common.pagination.PageSize;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CallHistoryService {

    private final CallRepository callRepository;
    private final AnalysisSummaryProvider analysisSummaryProvider;

    @Transactional(readOnly = true)
    public CallsResponse getCallsByUserId(final Long userId, final int page, final int size) {
        final PageSize pageSize = PageSize.clamp(size);
        final Slice<CallSummaryProjection> slice = callRepository.findEndedSummariesByUserId(
                userId, PageRequest.of(page, pageSize.value()));
        final List<Long> callIds = slice.getContent().stream()
                .map(CallSummaryProjection::getId)
                .toList();
        final Map<Long, AnalysisSummaryView> analysisSummaryByCallId =
                analysisSummaryProvider.findByCallIds(userId, callIds);
        return CallsResponse.from(slice, analysisSummaryByCallId);
    }
}
