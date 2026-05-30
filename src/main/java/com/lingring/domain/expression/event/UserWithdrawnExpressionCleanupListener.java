package com.lingring.domain.expression.event;

import com.lingring.domain.expression.dao.UserExpressionRepository;
import com.lingring.domain.user.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserWithdrawnExpressionCleanupListener {

    private final UserExpressionRepository userExpressionRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(final UserWithdrawnEvent event) {
        userExpressionRepository.deleteByUserId(event.userId());
    }
}
