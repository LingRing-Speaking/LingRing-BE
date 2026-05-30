package com.lingring.domain.moderation.event;

import com.lingring.domain.moderation.dao.UserBlockRepository;
import com.lingring.domain.moderation.dao.UserReportRepository;
import com.lingring.domain.user.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserWithdrawnModerationCleanupListener {

    private final UserBlockRepository userBlockRepository;
    private final UserReportRepository userReportRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(final UserWithdrawnEvent event) {
        userBlockRepository.deleteByUserId(event.userId());
        userReportRepository.anonymizeReporter(event.userId());
    }
}
