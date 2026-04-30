package com.lingring.domain.signaling.api;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.web.socket.WebSocketSession;

public final class SignalingSessionAttributes {

    public static final String USER_ID = "userId";
    public static final String ROOM_ID = "roomId";

    private SignalingSessionAttributes() {
    }

    public static void putUserId(final Map<String, Object> attributes, final Long userId) {
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        attributes.put(USER_ID, userId);
    }

    public static void putRoomId(final Map<String, Object> attributes, final UUID roomId) {
        Objects.requireNonNull(roomId, "roomId는 null일 수 없습니다.");
        attributes.put(ROOM_ID, roomId);
    }

    public static Long getUserId(final WebSocketSession session) {
        return Objects.requireNonNull(
                (Long) session.getAttributes().get(USER_ID),
                "USER_ID_ATTR이 세션에 없습니다. 핸드셰이크 인터셉터 인바리언트 위반.");
    }

    public static UUID getRoomId(final WebSocketSession session) {
        return Objects.requireNonNull(
                (UUID) session.getAttributes().get(ROOM_ID),
                "ROOM_ID_ATTR이 세션에 없습니다. 핸드셰이크 인터셉터 인바리언트 위반.");
    }
}
