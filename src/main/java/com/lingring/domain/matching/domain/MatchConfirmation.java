package com.lingring.domain.matching.domain;

import com.lingring.domain.matching.exception.MatchConfirmationNotFoundException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.NonNull;

public record MatchConfirmation(
        @NonNull Long userAId,
        @NonNull Long userBId,
        boolean userAAccepted,
        boolean userBAccepted,
        @NonNull UUID roomId,
        @NonNull LocalDateTime deadline
) {

    public static String pairKeyOf(final Long firstUserId, final Long secondUserId) {
        final Long a = Math.min(firstUserId, secondUserId);
        final Long b = Math.max(firstUserId, secondUserId);
        return a + ":" + b;
    }

    public String pairKey() {
        return pairKeyOf(userAId, userBId);
    }

    public boolean isExpired(final LocalDateTime now) {
        return !now.isBefore(deadline);
    }

    public boolean bothAccepted() {
        return userAAccepted && userBAccepted;
    }

    public Long partnerOf(final Long userId) {
        if (userAId.equals(userId)) {
            return userBId;
        }
        if (userBId.equals(userId)) {
            return userAId;
        }
        throw new MatchConfirmationNotFoundException(userId);
    }
}
