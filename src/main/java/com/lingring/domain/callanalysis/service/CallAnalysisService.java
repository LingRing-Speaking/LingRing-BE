package com.lingring.domain.callanalysis.service;

import com.lingring.domain.callanalysis.dao.CallAnalysisRepository;
import com.lingring.domain.callanalysis.domain.CallAnalysis;
import com.lingring.domain.callanalysis.domain.vo.AnalysisResult;
import com.lingring.domain.callanalysis.dto.response.CallAnalysisResponse;
import com.lingring.domain.callanalysis.dto.response.CallAnalysisStatusResponse;
import com.lingring.domain.callanalysis.exception.CallAnalysisAccessForbiddenException;
import com.lingring.domain.callanalysis.exception.CallAnalysisNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CallAnalysisService {

    private final CallAnalysisRepository callAnalysisRepository;

    @Transactional
    public CallAnalysis startProcessing(final Long callId, final Long userId) {
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
        final CallAnalysis analysis = findOwned(analysisId, requesterId);
        return CallAnalysisResponse.from(analysis);
    }

    @Transactional(readOnly = true)
    public CallAnalysisStatusResponse getStatus(final Long analysisId, final Long requesterId) {
        final CallAnalysis analysis = findOwned(analysisId, requesterId);
        return new CallAnalysisStatusResponse(analysis.getStatus());
    }

    private CallAnalysis findOwned(final Long analysisId, final Long requesterId) {
        final CallAnalysis analysis = callAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new CallAnalysisNotFoundException(analysisId));
        if (!analysis.isOwnedBy(requesterId)) {
            throw new CallAnalysisAccessForbiddenException(analysisId, requesterId);
        }
        return analysis;
    }
}
