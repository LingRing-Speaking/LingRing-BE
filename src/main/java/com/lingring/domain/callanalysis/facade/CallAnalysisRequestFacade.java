package com.lingring.domain.callanalysis.facade;

import com.lingring.domain.call.dto.response.CallTranscriptStartResponse;
import com.lingring.domain.call.service.CallTranscriptService;
import com.lingring.domain.call.service.CallTranscriptService.StartTranscriptResult;
import com.lingring.domain.callanalysis.event.CallAnalysisRequestedEvent;
import com.lingring.domain.callanalysis.service.CallAnalysisService;
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
    public CallTranscriptStartResponse request(final Long callId, final Long userId) {
        final StartTranscriptResult result = callTranscriptService.startTranscript(callId, userId);
        if (!result.freshlyCreated()) {
            return CallTranscriptStartResponse.from(result.transcript());
        }

        callAnalysisService.startProcessing(callId, result.userAId());
        callAnalysisService.startProcessing(callId, result.userBId());
        eventPublisher.publishEvent(new CallAnalysisRequestedEvent(callId, result.recordings()));

        return CallTranscriptStartResponse.from(result.transcript());
    }
}
