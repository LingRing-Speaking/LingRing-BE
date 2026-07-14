package com.lingring.domain.matching.domain;

import java.util.UUID;
import lombok.NonNull;

public record CallInvitationResult(
        @NonNull CallInvitationPollStatus status,
        UUID roomId,
        Long callId
) {

    public static CallInvitationResult accepted(@NonNull final UUID roomId, @NonNull final Long callId) {
        return new CallInvitationResult(CallInvitationPollStatus.ACCEPTED, roomId, callId);
    }

    public static CallInvitationResult declined() {
        return new CallInvitationResult(CallInvitationPollStatus.DECLINED, null, null);
    }
}
