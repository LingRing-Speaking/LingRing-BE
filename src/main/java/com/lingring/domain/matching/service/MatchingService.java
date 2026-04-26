package com.lingring.domain.matching.service;

import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchingCandidate;
import com.lingring.domain.matching.domain.policy.MatchingPolicy;
import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingQueueRepository matchingQueueRepository;
    private final List<MatchingPolicy> matchingPolicies;
    private final DateTimeProvider dateTimeProvider;

    public MatchingStatusResponse enterQueue(final Long userId) {
        matchingQueueRepository.clearResult(userId);

        final LocalDateTime now = dateTimeProvider.now();
        final MatchingCandidate self = new MatchingCandidate(userId, now);
        final Optional<MatchingCandidate> partner = findCompatiblePartner(self);

        if (partner.isEmpty()) {
            matchingQueueRepository.enqueue(userId, now);
            return MatchingStatusResponse.waiting();
        }

        final Long partnerId = partner.get().userId();
        matchingQueueRepository.remove(partnerId);
        matchingQueueRepository.remove(userId);
        matchingQueueRepository.saveResult(userId, partnerId);
        matchingQueueRepository.saveResult(partnerId, userId);
        return MatchingStatusResponse.matched(partnerId);
    }

    public MatchingStatusResponse getStatus(final Long userId) {
        final Optional<Long> result = matchingQueueRepository.findResult(userId);
        if (result.isPresent()) {
            return MatchingStatusResponse.matched(result.get());
        }
        if (matchingQueueRepository.contains(userId)) {
            return MatchingStatusResponse.waiting();
        }
        return MatchingStatusResponse.none();
    }

    public void leaveQueue(final Long userId) {
        matchingQueueRepository.remove(userId);
    }

    private Optional<MatchingCandidate> findCompatiblePartner(final MatchingCandidate self) {
        final List<Predicate<MatchingCandidate>> filters = new ArrayList<>(matchingPolicies.size());
        for (final MatchingPolicy policy : matchingPolicies) {
            filters.add(policy.filterFor(self));
        }
        final List<MatchingCandidate> candidates = matchingQueueRepository.findAllOrderByEnqueuedAt();
        for (final MatchingCandidate candidate : candidates) {
            if (candidate.userId().equals(self.userId())) {
                continue;
            }
            if (passesAllFilters(filters, candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private boolean passesAllFilters(
            final List<Predicate<MatchingCandidate>> filters,
            final MatchingCandidate candidate
    ) {
        for (final Predicate<MatchingCandidate> filter : filters) {
            if (!filter.test(candidate)) {
                return false;
            }
        }
        return true;
    }
}
