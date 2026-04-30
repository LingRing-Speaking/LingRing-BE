package com.lingring.domain.signaling.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
    private final ConcurrentMap<Long, ScheduledFuture<?>> pendingByUserId = new ConcurrentHashMap<>();

    public DisconnectScheduler(
            final TaskScheduler taskScheduler,
            @Value("${signaling.disconnect-grace-period:10s}") final Duration gracePeriod) {
        this.taskScheduler = taskScheduler;
        this.gracePeriod = gracePeriod;
    }

    public void schedule(final Long userId, final Runnable task) {
        final ScheduledFuture<?> future = taskScheduler.schedule(() -> {
            pendingByUserId.remove(userId);
            try {
                task.run();
            } catch (final RuntimeException e) {
                log.warn("Disconnect task failed for userId={}: {}", userId, e.getMessage());
            }
        }, Instant.now().plus(gracePeriod));
        final ScheduledFuture<?> previous = pendingByUserId.put(userId, future);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    public void cancel(final Long userId) {
        final ScheduledFuture<?> previous = pendingByUserId.remove(userId);
        if (previous != null) {
            previous.cancel(false);
        }
    }
}