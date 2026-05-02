package com.lingring.domain.matching.service;

import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.MatchingResult;
import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.global.util.DateTimeProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingQueueRepository matchingQueueRepository;
    private final DateTimeProvider dateTimeProvider;

    public void enterQueue(final Long userId) {
        matchingQueueRepository.clearResult(userId);
        matchingQueueRepository.enqueue(userId, dateTimeProvider.now());
    }

    public MatchingStatusResponse getStatus(final Long userId) {
        final Optional<MatchingResult> result = matchingQueueRepository.findResult(userId);
        if (result.isPresent()) {
            final MatchingResult matched = result.get();
            return MatchingStatusResponse.matched(matched.partnerId(), matched.roomId());
        }
        if (matchingQueueRepository.contains(userId)) {
            return MatchingStatusResponse.waiting();
        }
        return MatchingStatusResponse.none();
    }

    public void leaveQueue(final Long userId) {
        matchingQueueRepository.remove(userId);
    }
}
