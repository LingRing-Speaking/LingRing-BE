package com.lingring.domain.matching.dto.response;

import com.lingring.domain.matching.domain.MatchingPollStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record MatchingStatusResponse(
        MatchingPollStatus status,
        Long partnerId,
        UUID roomId,
        LocalDateTime confirmDeadline,
        Long callId
) {

    public static MatchingStatusResponse waiting() {
        return new MatchingStatusResponse(MatchingPollStatus.WAITING, null, null, null, null);
    }

    public static MatchingStatusResponse awaitingConfirm(final Long partnerId, final LocalDateTime confirmDeadline) {
        return new MatchingStatusResponse(MatchingPollStatus.AWAITING_CONFIRM, partnerId, null, confirmDeadline, null);
    }

    public static MatchingStatusResponse matched(final Long partnerId, final UUID roomId, final Long callId) {
        return new MatchingStatusResponse(MatchingPollStatus.MATCHED, partnerId, roomId, null, callId);
    }

    public static MatchingStatusResponse none() {
        return new MatchingStatusResponse(MatchingPollStatus.NONE, null, null, null, null);
    }
}