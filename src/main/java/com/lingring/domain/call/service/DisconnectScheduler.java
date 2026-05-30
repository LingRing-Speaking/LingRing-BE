package com.lingring.domain.call.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DisconnectScheduler {

    private final TaskScheduler taskScheduler;
    private final Duration gracePeriod;
    private final PendingDisconnects pending = new PendingDisconnects();

    public DisconnectScheduler(
            final TaskScheduler taskScheduler,
            @Value("${signaling.disconnect-grace-period:10s}") final Duration gracePeriod
    ) {
        this.taskScheduler = taskScheduler;
        this.gracePeriod = gracePeriod;
    }

    public void schedule(final Long userId, final Runnable task) {
        final ScheduledFuture<?> future = taskScheduler.schedule(
                () -> runAndCleanup(userId, task),
                Instant.now().plus(gracePeriod)
        );
        pending.register(userId, () -> future.cancel(false));
    }

    public void cancel(final Long userId) {
        pending.cancel(userId);
    }

    private void runAndCleanup(final Long userId, final Runnable task) {
        pending.clear(userId);
        try {
            task.run();
        } catch (final RuntimeException e) {
            log.warn("Disconnect task failed for userId={}: {}", userId, e.getMessage());
        }
    }
}
