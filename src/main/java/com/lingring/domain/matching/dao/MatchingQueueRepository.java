package com.lingring.domain.matching.dao;

import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.domain.matching.domain.MatchingResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchingQueueRepository {

    void enqueue(Long userId, LocalDateTime enqueuedAt);

    void remove(Long userId);

    boolean contains(Long userId);

    List<MatchingCandidate> findAllOrderByEnqueuedAt();

    void saveResult(Long userId, Long partnerId, UUID roomId);

    boolean commitMatch(Long userId, Long partnerId, UUID roomId);

    Optional<MatchingResult> findResult(Long userId);

    void clearResult(Long userId);
}
