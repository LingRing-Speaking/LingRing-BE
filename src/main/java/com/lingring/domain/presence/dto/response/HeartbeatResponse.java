package com.lingring.domain.presence.dto.response;

import com.lingring.domain.matching.dto.response.IncomingCallInvitationResponse;

public record HeartbeatResponse(
        IncomingCallInvitationResponse incomingInvitation
) {

    public static HeartbeatResponse of(final IncomingCallInvitationResponse incomingInvitation) {
        return new HeartbeatResponse(incomingInvitation);
    }

    public static HeartbeatResponse empty() {
        return new HeartbeatResponse(null);
    }
}
