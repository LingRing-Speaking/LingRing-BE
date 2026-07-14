package com.lingring.domain.presence.facade;

import com.lingring.domain.matching.dto.response.IncomingCallInvitationResponse;
import com.lingring.domain.matching.service.CallInvitationService;
import com.lingring.domain.presence.dto.response.HeartbeatResponse;
import com.lingring.domain.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PresenceFacade {

    private final PresenceService presenceService;
    private final CallInvitationService callInvitationService;

    public HeartbeatResponse heartbeat(final Long userId) {
        presenceService.heartbeat(userId);
        return callInvitationService.findIncoming(userId)
                .map(invitation -> HeartbeatResponse.of(IncomingCallInvitationResponse.from(invitation)))
                .orElseGet(HeartbeatResponse::empty);
    }
}
