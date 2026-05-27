package com.lingring.domain.user.event;

import com.lingring.domain.user.service.WithdrawalLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WithdrawalLogListener {

    private final WithdrawalLogService withdrawalLogService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(final UserWithdrawnEvent event) {
        withdrawalLogService.record(event.reason(), event.description());
    }
}
