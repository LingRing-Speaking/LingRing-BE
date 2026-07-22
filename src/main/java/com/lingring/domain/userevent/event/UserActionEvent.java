package com.lingring.domain.userevent.event;

import com.lingring.domain.userevent.domain.EventName;
import com.lingring.domain.userevent.domain.UserEvent;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record UserActionEvent(
        Long userId,
        EventName eventName,
        LocalDateTime occurredAt,
        Map<String, Object> properties
) {

    private static final String WAIT_MS = "wait_ms";
    private static final String ROOM_ID = "room_id";
    private static final String REASON = "reason";
    private static final String REQUEUED = "requeued";
    private static final String DURATION_SEC = "duration_sec";
    private static final String INVITEE_ID = "invitee_id";
    private static final String INVITER_ID = "inviter_id";

    public static UserActionEvent matchingRequested(final Long userId, final LocalDateTime occurredAt) {
        return new UserActionEvent(userId, EventName.MATCHING_REQUESTED, occurredAt, null);
    }

    public static UserActionEvent matchingMatched(
            final Long userId,
            final long waitMs,
            final UUID roomId,
            final LocalDateTime occurredAt
    ) {
        return new UserActionEvent(userId, EventName.MATCHING_MATCHED, occurredAt,
                Map.of(WAIT_MS, waitMs, ROOM_ID, roomId.toString()));
    }

    public static UserActionEvent matchingCancelled(
            final Long userId,
            final long waitMs,
            final LocalDateTime occurredAt
    ) {
        return new UserActionEvent(userId, EventName.MATCHING_CANCELLED, occurredAt, Map.of(WAIT_MS, waitMs));
    }

    public static UserActionEvent matchingFailed(
            final Long userId,
            final String reason,
            final long waitMs,
            final boolean requeued,
            final LocalDateTime occurredAt
    ) {
        return new UserActionEvent(userId, EventName.MATCHING_FAILED, occurredAt,
                Map.of(REASON, reason, WAIT_MS, waitMs, REQUEUED, requeued));
    }

    public static UserActionEvent invitationSent(
            final Long inviterId,
            final Long inviteeId,
            final LocalDateTime occurredAt
    ) {
        return new UserActionEvent(inviterId, EventName.INVITATION_SENT, occurredAt, Map.of(INVITEE_ID, inviteeId));
    }

    public static UserActionEvent invitationAccepted(
            final Long inviteeId,
            final Long inviterId,
            final UUID roomId,
            final LocalDateTime occurredAt
    ) {
        return new UserActionEvent(inviteeId, EventName.INVITATION_ACCEPTED, occurredAt,
                Map.of(INVITER_ID, inviterId, ROOM_ID, roomId.toString()));
    }

    public static UserActionEvent invitationDeclined(
            final Long inviteeId,
            final Long inviterId,
            final LocalDateTime occurredAt
    ) {
        return new UserActionEvent(inviteeId, EventName.INVITATION_DECLINED, occurredAt, Map.of(INVITER_ID, inviterId));
    }

    public static UserActionEvent invitationCancelled(final Long inviterId, final LocalDateTime occurredAt) {
        return new UserActionEvent(inviterId, EventName.INVITATION_CANCELLED, occurredAt, null);
    }

    public static UserActionEvent callEnded(
            final Long userId,
            final String reason,
            final long durationSec,
            final UUID roomId,
            final LocalDateTime occurredAt
    ) {
        return new UserActionEvent(userId, EventName.CALL_ENDED, occurredAt,
                Map.of(REASON, reason, DURATION_SEC, durationSec, ROOM_ID, roomId.toString()));
    }

    public UserEvent toEntity() {
        return UserEvent.record(userId, eventName, occurredAt, properties);
    }
}
