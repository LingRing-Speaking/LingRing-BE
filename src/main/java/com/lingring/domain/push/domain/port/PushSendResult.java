package com.lingring.domain.push.domain.port;

import java.util.List;

public record PushSendResult(
        List<String> invalidTokens
) {

    public static PushSendResult allDelivered() {
        return new PushSendResult(List.of());
    }

    public static PushSendResult withInvalidTokens(final List<String> invalidTokens) {
        return new PushSendResult(List.copyOf(invalidTokens));
    }

    public boolean hasInvalidTokens() {
        return !invalidTokens.isEmpty();
    }
}
