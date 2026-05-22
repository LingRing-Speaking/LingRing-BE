package com.lingring.domain.call.event;

import com.lingring.domain.call.domain.TranscriptionStarter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallTranscriptRequestedListener {

    private final TranscriptionStarter transcriptionStarter;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(final CallTranscriptRequestedEvent event) {
        try {
            transcriptionStarter.requestTranscript(event.callId(), event.recordings());
        } catch (final RuntimeException e) {
            log.error("transcript 변환 요청 실패: callId={}", event.callId(), e);
        }
    }
}
