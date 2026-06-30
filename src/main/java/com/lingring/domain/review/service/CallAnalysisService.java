package com.lingring.domain.review.service;

import com.lingring.domain.review.dao.CallAnalysisRepository;
import com.lingring.domain.review.dao.dto.CallAnalysisSummaryProjection;
import com.lingring.domain.review.domain.analysis.CallAnalysis;
import com.lingring.domain.review.domain.analysis.vo.AnalysisResult;
import com.lingring.domain.review.dto.response.CallAnalysisResponse;
import com.lingring.domain.review.dto.response.CallAnalysisStatusResponse;
import com.lingring.domain.review.exception.CallAnalysisAccessForbiddenException;
import com.lingring.domain.review.exception.CallAnalysisNotFoundException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CallAnalysisService {

    private final CallAnalysisRepository callAnalysisRepository;

    @Transactional
    public RequestResult requestForUser(final Long callId, final Long userId) {
        final CallAnalysis analysis = callAnalysisRepository.findByCallIdAndUserId(callId, userId)
                .orElseGet(() -> callAnalysisRepository.save(CallAnalysis.processing(callId, userId)));
        final boolean freshlyRequested = analysis.markRequestedIfAbsent();
        return new RequestResult(analysis, freshlyRequested);
    }

    @Transactional
    public CallAnalysis ensureExistsForUser(final Long callId, final Long userId) {
        return callAnalysisRepository.findByCallIdAndUserId(callId, userId)
                .orElseGet(() -> callAnalysisRepository.save(CallAnalysis.processing(callId, userId)));
    }

    @Transactional
    public void complete(
            final Long callId,
            final Long userId,
            final AnalysisResult result,
            final String modelIdentifier
    ) {
        final CallAnalysis analysis = callAnalysisRepository.findByCallIdAndUserId(callId, userId)
                .orElseThrow(() -> new CallAnalysisNotFoundException(callId, userId));
        analysis.complete(result, modelIdentifier);
    }

    @Transactional
    public void fail(final Long callId, final Long userId) {
        callAnalysisRepository.findByCallIdAndUserId(callId, userId)
                .ifPresent(CallAnalysis::fail);
    }

    @Transactional(readOnly = true)
    public CallAnalysisResponse get(final Long analysisId, final Long requesterId) {
        final CallAnalysis analysis = findOwnedAndRequested(analysisId, requesterId);
        return CallAnalysisResponse.from(analysis);
    }

    @Transactional(readOnly = true)
    public CallAnalysisStatusResponse getStatus(final Long analysisId, final Long requesterId) {
        final CallAnalysis analysis = findOwnedAndRequested(analysisId, requesterId);
        return new CallAnalysisStatusResponse(analysis.getStatus());
    }

    @Transactional(readOnly = true)
    public Map<Long, CallAnalysisSummary> findRequestedAnalysisSummariesByCallIds(
            final Long userId,
            final Collection<Long> callIds
    ) {
        if (callIds.isEmpty()) {
            return Map.of();
        }
        return callAnalysisRepository.findRequestedAnalysisSummaries(userId, callIds).stream()
                .collect(Collectors.toMap(
                        CallAnalysisSummaryProjection::getCallId,
                        p -> new CallAnalysisSummary(p.getAnalysisId(), p.getStatus())
                ));
    }

    private CallAnalysis findOwnedAndRequested(final Long analysisId, final Long requesterId) {
        final CallAnalysis analysis = callAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new CallAnalysisNotFoundException(analysisId));
        if (!analysis.isOwnedBy(requesterId)) {
            throw new CallAnalysisAccessForbiddenException(analysisId, requesterId);
        }
        if (!analysis.isRequested()) {
            throw new CallAnalysisNotFoundException(analysisId);
        }
        return analysis;
    }
}
