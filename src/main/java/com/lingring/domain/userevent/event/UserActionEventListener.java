package com.lingring.domain.userevent.event;

import com.lingring.domain.userevent.dao.UserEventRepository;
import com.lingring.global.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActionEventListener {

    private final UserEventRepository userEventRepository;

    // fallbackExecution=true — Redis만 만지는 발행점(매칭 큐·초대)은 트랜잭션 없이 발행되므로 즉시 수신한다
    // @Transactional 미부착 — save 실패 시 rollback-only 커밋 예외가 catch 밖으로 새어 log-only 보장이 깨진다
    @Async(AsyncConfig.USER_EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void record(final UserActionEvent event) {
        try {
            userEventRepository.save(event.toEntity());
        } catch (final Exception e) {
            log.warn("user_event 저장 실패: {}", event, e);
        }
    }
}
