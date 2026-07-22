package com.lingring.domain.matching.service;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.global.config.MatchingProperties;
import com.lingring.domain.matching.dao.MatchConfirmationRepository;
import com.lingring.domain.matching.dao.dto.AcceptOutcome;
import com.lingring.domain.matching.dao.dto.AcceptResult;
import com.lingring.domain.matching.dao.MatchingQueueRepository;
import com.lingring.domain.matching.dao.PairCooldownRepository;
import com.lingring.domain.matching.domain.MatchConfirmation;
import com.lingring.domain.matching.domain.MatchingResult;
import com.lingring.domain.matching.domain.MatchingFailReason;
import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.domain.matching.exception.MatchConfirmationNotFoundException;
import com.lingring.domain.userevent.event.UserActionEvent;
import com.lingring.global.util.DateTimeProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingQueueRepository matchingQueueRepository;
    private final MatchConfirmationRepository matchConfirmationRepository;
    private final PairCooldownRepository pairCooldownRepository;
    private final CallRepository callRepository;
    private final DateTimeProvider dateTimeProvider;
    private final MatchingProperties matchingProperties;
    private final ApplicationEventPublisher eventPublisher;

    public void enterQueue(final Long userId) {
        final LocalDateTime now = dateTimeProvider.now();
        matchingQueueRepository.clearResult(userId);
        matchingQueueRepository.enqueue(userId, now);
        matchingQueueRepository.markAlive(userId, matchingProperties.aliveTtl());
        eventPublisher.publishEvent(UserActionEvent.matchingRequested(userId, now));
    }

    public MatchingStatusResponse getStatus(final Long userId) {
        matchingQueueRepository.markAlive(userId, matchingProperties.aliveTtl());
        final Optional<MatchingResult> result = matchingQueueRepository.findResult(userId);
        if (result.isPresent()) {
            return matchedResponse(result.get());
        }
        final Optional<MatchConfirmation> confirmation = matchConfirmationRepository.findByUser(userId);
        if (confirmation.isPresent()) {
            return confirmationResponse(userId, confirmation.get());
        }
        return queuedResponse(userId);
    }

    private MatchingStatusResponse matchedResponse(final MatchingResult result) {
        final Long callId = callRepository.findByRoomId(result.roomId())
                .map(Call::getId)
                .orElse(null);
        if (callId == null) {
            // #179: 두 번째 수락자의 결과 승격(Redis)과 Call 커밋(DB) 사이에 폴링이 도착한 race.
            // callId 없는 MATCHED 를 내보내면 클라이언트가 녹음 없이 통화에 진입해 해당 통화의
            // 녹음이 유실된다. 다음 폴링(<1s)에서 완전한 MATCHED 를 받도록 이번 응답은 보류.
            return MatchingStatusResponse.waiting();
        }
        return MatchingStatusResponse.matched(result.partnerId(), result.roomId(), callId);
    }

    private MatchingStatusResponse confirmationResponse(final Long userId, final MatchConfirmation confirmation) {
        final LocalDateTime now = dateTimeProvider.now();
        if (confirmation.isExpired(now)) {
            cancelByDeadline(confirmation, now);
            return MatchingStatusResponse.waiting();
        }
        return MatchingStatusResponse.awaitingConfirm(confirmation.partnerOf(userId), confirmation.deadline());
    }

    private MatchingStatusResponse queuedResponse(final Long userId) {
        if (matchingQueueRepository.contains(userId)) {
            return MatchingStatusResponse.waiting();
        }
        return MatchingStatusResponse.none();
    }

    public void leaveQueue(final Long userId) {
        final Optional<LocalDateTime> enqueuedAt = matchingQueueRepository.findEnqueuedAt(userId);
        matchingQueueRepository.remove(userId);
        if (enqueuedAt.isEmpty()) {
            return;
        }
        final LocalDateTime now = dateTimeProvider.now();
        eventPublisher.publishEvent(UserActionEvent.matchingCancelled(userId, waitMs(enqueuedAt.get(), now), now));
    }

    public void acceptMatch(final Long userId) {
        final LocalDateTime now = dateTimeProvider.now();
        // MATCHED 승격 시 확정 레코드가 원자적으로 삭제되므로, wait_ms 계산용 확정 정보를 accept 전에 읽어둔다
        final Optional<MatchConfirmation> beforeAccept = matchConfirmationRepository.findByUser(userId);
        final AcceptResult result = matchConfirmationRepository.accept(userId, now);
        final AcceptOutcome outcome = result.outcome();
        if (outcome == AcceptOutcome.NOT_FOUND) {
            throw new MatchConfirmationNotFoundException(userId);
        }
        if (outcome == AcceptOutcome.EXPIRED) {
            matchConfirmationRepository.findByUser(userId)
                    .ifPresent(confirmation -> cancelByDeadline(confirmation, now));
            return;
        }
        if (outcome == AcceptOutcome.ACCEPTED_WAITING) {
            return;
        }
        // MATCHED
        final Call call = persistCallForMatched(userId, now);
        beforeAccept.ifPresent(confirmation -> publishMatched(confirmation, call.getRoomId(), now));
    }

    public void declineMatch(final Long userId) {
        final MatchConfirmation confirmation = matchConfirmationRepository.findByUser(userId)
                .orElseThrow(() -> new MatchConfirmationNotFoundException(userId));
        cancelByDecline(confirmation, userId);
    }

    public void expireOverdueConfirmations() {
        final LocalDateTime now = dateTimeProvider.now();
        final List<MatchConfirmation> expired = matchConfirmationRepository.findAllExpired(now);
        for (final MatchConfirmation c : expired) {
            cancelByDeadline(c, now);
        }
    }

    private Call persistCallForMatched(final Long userId, final LocalDateTime now) {
        final MatchingResult promoted = matchingQueueRepository.findResult(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Result missing after MATCHED promotion for userId=" + userId));
        return callRepository.save(Call.start(userId, promoted.partnerId(), promoted.roomId(), now));
    }

    // 마감 만료: 수락한 쪽은 원래 대기순서 보존, 무응답은 now로 강등
    private void cancelByDeadline(final MatchConfirmation confirmation, final LocalDateTime now) {
        removeWithCooldown(confirmation);
        requeueAfterDeadline(confirmation.userAId(), confirmation.userAAccepted(), confirmation.userAEnqueuedAt(), now);
        requeueAfterDeadline(confirmation.userBId(), confirmation.userBAccepted(), confirmation.userBEnqueuedAt(), now);
        publishFailed(confirmation.userAId(), MatchingFailReason.CONFIRM_TIMEOUT, confirmation.userAEnqueuedAt(), now);
        publishFailed(confirmation.userBId(), MatchingFailReason.CONFIRM_TIMEOUT, confirmation.userBEnqueuedAt(), now);
    }

    // 거절: 양쪽 모두 원래 대기순서 보존 (거절자=권리 행사, 상대=마감 전 중단된 피해자)
    private void cancelByDecline(final MatchConfirmation confirmation, final Long declinerId) {
        removeWithCooldown(confirmation);
        matchingQueueRepository.enqueue(confirmation.userAId(), confirmation.userAEnqueuedAt());
        matchingQueueRepository.enqueue(confirmation.userBId(), confirmation.userBEnqueuedAt());
        final LocalDateTime now = dateTimeProvider.now();
        publishFailed(confirmation.userAId(), declineReason(confirmation.userAId(), declinerId),
                confirmation.userAEnqueuedAt(), now);
        publishFailed(confirmation.userBId(), declineReason(confirmation.userBId(), declinerId),
                confirmation.userBEnqueuedAt(), now);
    }

    private MatchingFailReason declineReason(final Long userId, final Long declinerId) {
        if (userId.equals(declinerId)) {
            return MatchingFailReason.SELF_DECLINED;
        }
        return MatchingFailReason.PEER_DECLINED;
    }

    private void publishMatched(final MatchConfirmation confirmation, final UUID roomId, final LocalDateTime now) {
        eventPublisher.publishEvent(UserActionEvent.matchingMatched(
                confirmation.userAId(), waitMs(confirmation.userAEnqueuedAt(), now), roomId, now));
        eventPublisher.publishEvent(UserActionEvent.matchingMatched(
                confirmation.userBId(), waitMs(confirmation.userBEnqueuedAt(), now), roomId, now));
    }

    // 거절·마감 실패는 큐 재진입(requeued=true)이므로 종료가 아닌 "이번 페어링 시도 실패"로 기록된다
    private void publishFailed(final Long userId, final MatchingFailReason reason,
            final LocalDateTime enqueuedAt, final LocalDateTime now) {
        eventPublisher.publishEvent(UserActionEvent.matchingFailed(
                userId, reason.name(), waitMs(enqueuedAt, now), true, now));
    }

    private long waitMs(final LocalDateTime enqueuedAt, final LocalDateTime now) {
        return Duration.between(enqueuedAt, now).toMillis();
    }

    private void requeueAfterDeadline(final Long userId, final boolean accepted,
            final LocalDateTime originalEnqueuedAt, final LocalDateTime now) {
        if (accepted) {
            matchingQueueRepository.enqueue(userId, originalEnqueuedAt);
            return;
        }
        matchingQueueRepository.enqueue(userId, now);
    }

    private void removeWithCooldown(final MatchConfirmation confirmation) {
        pairCooldownRepository.put(confirmation.pairKey(), Duration.ofMinutes(matchingProperties.cooldownMinutes()));
        matchConfirmationRepository.delete(confirmation.pairKey());
    }
}
