package com.lingring.domain.user.event;

import com.lingring.domain.user.dao.WithdrawalLogRepository;
import com.lingring.domain.user.domain.WithdrawalLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WithdrawalLogListener {

    private final WithdrawalLogRepository withdrawalLogRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(final UserWithdrawnEvent event) {
        withdrawalLogRepository.save(WithdrawalLog.record(event.reason(), event.description()));
    }
}
