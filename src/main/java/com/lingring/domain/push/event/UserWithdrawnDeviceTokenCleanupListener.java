package com.lingring.domain.push.event;

import com.lingring.domain.push.dao.DeviceTokenRepository;
import com.lingring.domain.user.event.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserWithdrawnDeviceTokenCleanupListener {

    private final DeviceTokenRepository deviceTokenRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(final UserWithdrawnEvent event) {
        deviceTokenRepository.deleteAllByUserId(event.userId());
    }
}
