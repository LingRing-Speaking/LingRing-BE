package com.lingring.domain.matching.dto.response;

import com.lingring.domain.matching.domain.CallInvitation;
import java.time.LocalDateTime;

public record IncomingCallInvitationResponse(
        Long inviterId,
        LocalDateTime deadline
) {

    public static IncomingCallInvitationResponse from(final CallInvitation invitation) {
        return new IncomingCallInvitationResponse(invitation.inviterId(), invitation.deadline());
    }
}
