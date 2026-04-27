package com.lingring.domain.matching.dto.response;

import com.lingring.domain.matching.domain.MatchStatus;

public record MatchingStatusResponse(
        MatchStatus status,
        Long partnerId
) {

    public static MatchingStatusResponse waiting() {
        return new MatchingStatusResponse(MatchStatus.WAITING, null);
    }

    public static MatchingStatusResponse matched(final Long partnerId) {
        return new MatchingStatusResponse(MatchStatus.MATCHED, partnerId);
    }

    public static MatchingStatusResponse none() {
        return new MatchingStatusResponse(MatchStatus.NONE, null);
    }
}
