package com.lingring.domain.matching.dao;

import com.lingring.domain.matching.domain.MatchingCandidate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchingQueueRepository {

    void enqueue(Long userId, LocalDateTime enqueuedAt);

    void remove(Long userId);

    boolean contains(Long userId);

    List<MatchingCandidate> findAllOrderByEnqueuedAt();

    void saveResult(Long userId, Long partnerId);

    boolean commitMatch(Long userId, Long partnerId);

    Optional<Long> findResult(Long userId);

    void clearResult(Long userId);
}
