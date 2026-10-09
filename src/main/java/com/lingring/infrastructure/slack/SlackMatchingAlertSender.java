package com.lingring.infrastructure.slack;

import com.lingring.domain.matching.domain.port.MatchingAlertSender;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackMatchingAlertSender implements MatchingAlertSender {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String QUEUE_ENTERED_TEXT = "🔔 매칭 대기 진입 — userId=%d (%s)";

    private final RestClient slackRestClient;
    private final SlackProperties properties;

    @Override
    public void sendQueueEntered(final Long userId, final LocalDateTime enteredAt) {
        if (!properties.hasMatchingWebhookUrl()) {
            return;
        }
        final String text = QUEUE_ENTERED_TEXT.formatted(userId, enteredAt.format(TIME_FORMAT));
        try {
            slackRestClient.post()
                    .uri(properties.matchingWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SlackWebhookRequest(text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (final RestClientException e) {
            log.warn("Slack 매칭 대기 알림 발송 실패. userId={}", userId, e);
        }
    }
}
