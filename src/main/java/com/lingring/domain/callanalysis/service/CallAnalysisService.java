package com.lingring.domain.callanalysis.service;

import com.lingring.domain.callanalysis.dao.CallAnalysisRepository;
import com.lingring.domain.callanalysis.domain.CallAnalysis;
import com.lingring.domain.callanalysis.domain.vo.AnalysisResult;
import com.lingring.domain.callanalysis.dto.response.CallAnalysisResponse;
import com.lingring.domain.callanalysis.dto.response.CallAnalysisStatusResponse;
import com.lingring.domain.callanalysis.exception.CallAnalysisNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CallAnalysisService {

    private final CallAnalysisRepository callAnalysisRepository;

    @Transactional
    public void startProcessing(final Long callId, final Long userId) {
        if (callAnalysisRepository.existsByCallIdAndUserId(callId, userId)) {
            return;
        }
        callAnalysisRepository.save(CallAnalysis.processing(callId, userId));
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
    public CallAnalysisResponse get(final Long callId, final Long userId) {
        final CallAnalysis analysis = callAnalysisRepository.findByCallIdAndUserId(callId, userId)
                .orElseThrow(() -> new CallAnalysisNotFoundException(callId, userId));
        return CallAnalysisResponse.from(analysis);
    }

    @Transactional(readOnly = true)
    public CallAnalysisStatusResponse getStatus(final Long callId, final Long userId) {
        final CallAnalysis analysis = callAnalysisRepository.findByCallIdAndUserId(callId, userId)
                .orElseThrow(() -> new CallAnalysisNotFoundException(callId, userId));
        return new CallAnalysisStatusResponse(analysis.getStatus());
    }
}
