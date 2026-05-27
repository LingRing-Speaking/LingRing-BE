package com.lingring.domain.call.domain;

import tools.jackson.databind.JsonNode;

public record SignalingMessage(
        SignalingMessageType type,
        Long fromUserId,
        Long toUserId,
        JsonNode payload
) {

    public SignalingMessage withRouting(final Long fromUserId, final Long toUserId) {
        return new SignalingMessage(type, fromUserId, toUserId, payload);
    }
}
