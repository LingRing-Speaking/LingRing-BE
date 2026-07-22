package com.lingring.domain.userevent.domain;

public enum EventName {

    // Matching
    MATCHING_REQUESTED,
    MATCHING_MATCHED,
    MATCHING_CANCELLED,
    MATCHING_FAILED,

    // Call Invitation
    INVITATION_SENT,
    INVITATION_ACCEPTED,
    INVITATION_DECLINED,
    INVITATION_CANCELLED,

    // Call
    CALL_ENDED
}
