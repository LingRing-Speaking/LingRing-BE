package com.lingring.domain.matching.domain;

public enum MatchingFailReason {

    // 본인이 매칭 제안을 거절
    SELF_DECLINED,

    // 상대가 매칭 제안을 거절
    PEER_DECLINED,

    // 수락 마감시간 초과
    CONFIRM_TIMEOUT,

    // 대기 중 이탈 (alive 키 만료로 큐에서 제거)
    CONNECTION_LOST
}
