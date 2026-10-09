package com.lingring.infrastructure.slack;

import com.lingring.domain.matching.domain.port.MatchingAlertSender;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FakeMatchingAlertSender implements MatchingAlertSender {

    private final List<SentQueueEnteredAlert> sent = new ArrayList<>();

    @Override
    public void sendQueueEntered(final Long userId, final LocalDateTime enteredAt) {
        sent.add(new SentQueueEnteredAlert(userId, enteredAt));
    }

    public List<SentQueueEnteredAlert> sent() {
        return List.copyOf(sent);
    }

    public record SentQueueEnteredAlert(
            Long userId,
            LocalDateTime enteredAt
    ) {
    }
}
