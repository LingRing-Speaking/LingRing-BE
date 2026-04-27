package com.lingring.domain.signaling.domain;

import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record SignalingMessage(
        SignalingMessageType type,
        UUID roomId,
        Long fromUserId,
        Long toUserId,
        JsonNode payload
) {

    public SignalingMessage withRouting(final Long fromUserId, final Long toUserId) {
        return new SignalingMessage(type, roomId, fromUserId, toUserId, payload);
    }
}
