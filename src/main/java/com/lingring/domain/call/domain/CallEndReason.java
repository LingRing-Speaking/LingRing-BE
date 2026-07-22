package com.lingring.domain.call.domain;

public enum CallEndReason {

    // 클라이언트가 명시적으로 종료 (정상 종료)
    HANGUP,

    // 연결 끊김 후 grace period 내 미복귀 (중도이탈)
    DISCONNECTED
}
