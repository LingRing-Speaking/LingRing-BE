package com.lingring.domain.review.event;

import com.lingring.domain.review.domain.port.CallAnalysisStarter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallAnalysisRequestedListener {

    private final CallAnalysisStarter callAnalysisStarter;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(final CallAnalysisRequestedEvent event) {
        try {
            callAnalysisStarter.requestAnalysis(event.callId(), event.recordings());
        } catch (final RuntimeException e) {
            log.error("call analysis 요청 실패: callId={}", event.callId(), e);
        }
    }
}
