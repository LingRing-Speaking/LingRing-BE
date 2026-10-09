package com.lingring.domain.matching.domain.port;

import java.time.LocalDateTime;

public interface MatchingAlertSender {

    void sendQueueEntered(Long userId, LocalDateTime enteredAt);
}
