package com.lingring.domain.matching.service;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.domain.matching.domain.MatchingQueue;
import com.lingring.domain.matching.domain.policy.MatchingPolicies;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingExecutor {

    private static final int MATCHING_SIZE = 2;

    private final MatchingQueueRepository matchingQueueRepository;
    private final CallRepository callRepository;
    private final MatchingPolicies matchingPolicies;
    private final RoomIdGenerator roomIdGenerator;
    private final DateTimeProvider dateTimeProvider;

    public void executeRound() {
        final List<MatchingCandidate> candidates = matchingQueueRepository.findAllOrderByEnqueuedAt();
        if (candidates.size() < MATCHING_SIZE) {
            return;
        }
        final MatchingQueue queue = new MatchingQueue(candidates);
        final Set<Long> consumed = new HashSet<>();

        for (final MatchingCandidate self : candidates) {
            checkAndExecute(self, consumed, queue);
        }
    }

    private void checkAndExecute(final MatchingCandidate self, final Set<Long> consumed, final MatchingQueue queue) {
        if (consumed.contains(self.userId())) {
            return;
        }
        final Optional<MatchingCandidate> partner = queue.findPartnerFor(self, matchingPolicies, consumed);
        if (partner.isEmpty()) {
            return;
        }
        final Long partnerId = partner.get().userId();
        final UUID roomId = roomIdGenerator.generate();
        final boolean committed = matchingQueueRepository.commitMatch(self.userId(), partnerId, roomId);
        if (!committed) {
            return;
        }
        final LocalDateTime now = dateTimeProvider.now();
        callRepository.save(Call.start(self.userId(), partnerId, roomId, now));
        consumed.add(self.userId());
        consumed.add(partnerId);
    }
}