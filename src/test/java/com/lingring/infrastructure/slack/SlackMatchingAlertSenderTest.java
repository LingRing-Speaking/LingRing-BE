package com.lingring.infrastructure.slack;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SlackMatchingAlertSenderTest {

    private static final String WEBHOOK_URL = "https://hooks.slack.com/services/T000/B000/XXXX";
    private static final Long USER_ID = 123L;
    private static final LocalDateTime ENTERED_AT = LocalDateTime.of(2026, 10, 9, 14, 2, 11);

    private MockRestServiceServer server;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        final RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    private SlackMatchingAlertSender senderWith(final String webhookUrl) {
        return new SlackMatchingAlertSender(restClient, new SlackProperties(webhookUrl));
    }

    @Nested
    @DisplayName("sendQueueEntered: 매칭 대기 진입 Slack 알림")
    class SendQueueEntered {

        @Test
        @DisplayName("웹훅 URL로 진입 유저와 시각이 담긴 메시지를 POST한다")
        void sendQueueEntered_postsMessageToWebhook() {
            // given
            server.expect(requestTo(WEBHOOK_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(jsonPath("$.text").value("🔔 매칭 대기 진입 — userId=123 (2026-10-09 14:02:11)"))
                    .andRespond(withSuccess());

            // when
            senderWith(WEBHOOK_URL).sendQueueEntered(USER_ID, ENTERED_AT);

            // then
            server.verify();
        }

        @Test
        @DisplayName("웹훅 URL이 비어 있으면 요청을 보내지 않는다 (prod 외 환경)")
        void sendQueueEntered_whenWebhookUrlBlank_sendsNothing() {
            // when
            senderWith("").sendQueueEntered(USER_ID, ENTERED_AT);

            // then
            server.verify();
        }

        @Test
        @DisplayName("웹훅 URL 설정이 없으면 요청을 보내지 않는다")
        void sendQueueEntered_whenWebhookUrlMissing_sendsNothing() {
            // when
            senderWith(null).sendQueueEntered(USER_ID, ENTERED_AT);

            // then
            server.verify();
        }

        @Test
        @DisplayName("Slack이 오류로 응답해도 예외를 던지지 않는다 (best-effort)")
        void sendQueueEntered_whenSlackFails_doesNotThrow() {
            // given
            server.expect(requestTo(WEBHOOK_URL))
                    .andRespond(withServerError());

            // when & then
            assertThatCode(() -> senderWith(WEBHOOK_URL).sendQueueEntered(USER_ID, ENTERED_AT))
                    .doesNotThrowAnyException();
            server.verify();
        }
    }
}
