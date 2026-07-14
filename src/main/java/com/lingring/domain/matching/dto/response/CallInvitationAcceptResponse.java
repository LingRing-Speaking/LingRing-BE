package com.lingring.domain.matching.dto.response;

import java.util.UUID;

public record CallInvitationAcceptResponse(
        UUID roomId,
        Long callId
) {
}
