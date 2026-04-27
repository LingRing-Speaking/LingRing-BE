package com.lingring.domain.matching.service;

import com.lingring.domain.matching.dao.MatchRepository;
import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.domain.Match;
import com.lingring.domain.matching.domain.MatchingResult;
import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.domain.matching.exception.MatchNotFoundException;
import com.lingring.global.util.DateTimeProvider;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingQueueRepository matchingQueueRepository;
    private final MatchRepository matchRepository;
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

    @Transactional
    public void endMatch(final UUID roomId) {
        final Match match = matchRepository.findByRoomId(roomId)
                .orElseThrow(() -> new MatchNotFoundException(roomId));
        match.end(dateTimeProvider.now());
    }
}