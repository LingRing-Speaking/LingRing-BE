package com.lingring.domain.matching.dto.response;

import com.lingring.domain.matching.domain.CallInvitationPollStatus;
import com.lingring.domain.matching.domain.CallInvitationResult;
import java.util.UUID;

public record CallInvitationStatusResponse(
        CallInvitationPollStatus status,
        UUID roomId,
        Long callId
) {

    public static CallInvitationStatusResponse from(final CallInvitationResult result) {
        return new CallInvitationStatusResponse(result.status(), result.roomId(), result.callId());
    }

    public static CallInvitationStatusResponse ringing() {
        return new CallInvitationStatusResponse(CallInvitationPollStatus.RINGING, null, null);
    }

    public static CallInvitationStatusResponse none() {
        return new CallInvitationStatusResponse(CallInvitationPollStatus.NONE, null, null);
    }
}
