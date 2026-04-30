package com.lingring.infrastructure.redis;

import java.util.UUID;

public final class SignalingChannels {

    public static final String PREFIX = "signaling:room:";
    public static final String PATTERN = PREFIX + "*";

    private SignalingChannels() {
    }

    public static String forRoom(final UUID roomId) {
        return PREFIX + roomId;
    }

    public static String joinedSetKey(final UUID roomId) {
        return PREFIX + roomId + ":joined";
    }

    public static String readyLockKey(final UUID roomId) {
        return PREFIX + roomId + ":ready_lock";
    }
}
