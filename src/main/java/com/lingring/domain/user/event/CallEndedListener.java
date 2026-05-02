package com.lingring.domain.user.event;

import com.lingring.domain.call.event.CallEndedEvent;
import com.lingring.domain.user.service.UserStatsService;
import com.lingring.global.util.DateTimeProvider;
import java.time.Duration;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CallEndedListener {

    private final UserStatsService userStatsService;
    private final DateTimeProvider dateTimeProvider;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(final CallEndedEvent event) {
        final Duration duration = Duration.between(event.startedAt(), event.endedAt());
        final LocalDate today = dateTimeProvider.now().toLocalDate();
        userStatsService.recordCompletedCall(event.userAId(), duration, today);
        userStatsService.recordCompletedCall(event.userBId(), duration, today);
    }
}
