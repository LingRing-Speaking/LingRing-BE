package com.lingring.domain.review.facade;

import com.lingring.domain.review.service.CallTranscriptService;
import com.lingring.domain.review.service.CallTranscriptService.StartTranscriptResult;
import com.lingring.domain.review.domain.analysis.CallAnalysis;
import com.lingring.domain.review.dto.response.CallAnalysisStartResponse;
import com.lingring.domain.review.event.CallAnalysisRequestedEvent;
import com.lingring.domain.review.service.CallAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CallAnalysisRequestFacade {

    private final CallTranscriptService callTranscriptService;
    private final CallAnalysisService callAnalysisService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CallAnalysisStartResponse request(final Long callId, final Long userId) {
        final StartTranscriptResult result = callTranscriptService.startTranscript(callId, userId);

        final CallAnalysis selfAnalysis = callAnalysisService.requestForUser(callId, userId);
        final Long otherUserId = result.userAId().equals(userId) ? result.userBId() : result.userAId();
        callAnalysisService.ensureExistsForUser(callId, otherUserId);

        if (result.freshlyCreated()) {
            eventPublisher.publishEvent(new CallAnalysisRequestedEvent(callId, result.recordings()));
        }

        return new CallAnalysisStartResponse(selfAnalysis.getId());
    }
}
