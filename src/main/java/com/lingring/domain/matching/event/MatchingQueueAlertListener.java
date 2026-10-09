package com.lingring.domain.matching.event;

import com.lingring.domain.matching.domain.port.MatchingAlertSender;
import com.lingring.domain.userevent.domain.EventName;
import com.lingring.domain.userevent.event.UserActionEvent;
import com.lingring.global.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MatchingQueueAlertListener {

    private final MatchingAlertSender matchingAlertSender;

    // fallbackExecution=true — 매칭 큐 진입은 Redis만 만지므로 트랜잭션 없이 발행된다
    @Async(AsyncConfig.USER_EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(final UserActionEvent event) {
        if (event.eventName() != EventName.MATCHING_REQUESTED) {
            return;
        }
        matchingAlertSender.sendQueueEntered(event.userId(), event.occurredAt());
    }
}
