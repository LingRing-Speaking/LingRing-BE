package com.lingring.domain.call.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class PendingDisconnects {

    private final ConcurrentMap<Long, Cancellable> tasks = new ConcurrentHashMap<>();

    public void register(final Long userId, final Cancellable task) {
        final Cancellable previous = tasks.put(userId, task);
        cancelIfPresent(previous);
    }

    public void cancel(final Long userId) {
        final Cancellable previous = tasks.remove(userId);
        cancelIfPresent(previous);
    }

    public void clear(final Long userId) {
        tasks.remove(userId);
    }

    private void cancelIfPresent(final Cancellable task) {
        if (task != null) {
            task.cancel();
        }
    }

    @FunctionalInterface
    interface Cancellable {
        void cancel();
    }
}