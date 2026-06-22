package com.lingring.domain.matching.dao;

import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.domain.matching.domain.MatchingResult;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchingQueueRepository {

    void enqueue(Long userId, LocalDateTime enqueuedAt);

    void remove(Long userId);

    boolean contains(Long userId);

    /**
     * 대기 중인 사용자의 생존(liveness)을 갱신한다. ttl 내 다시 호출되지 않으면 만료되어 이탈로 간주된다.
     * 큐 적재(enqueue)와 분리된 별도 키이므로, 시스템 재투입(requeue)으로는 갱신되지 않는다.
     */
    void markAlive(Long userId, Duration ttl);

    boolean isAlive(Long userId);

    List<MatchingCandidate> findAllOrderByEnqueuedAt();

    void saveResult(Long userId, Long partnerId, UUID roomId);

    boolean commitMatch(Long userId, Long partnerId, UUID roomId);

    Optional<MatchingResult> findResult(Long userId);

    void clearResult(Long userId);
}
