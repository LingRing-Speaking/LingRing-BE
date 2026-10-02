package com.lingring.domain.push.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.push.dao.DeviceTokenRepository;
import com.lingring.domain.push.domain.DeviceToken;
import com.lingring.domain.push.domain.Platform;
import com.lingring.domain.push.domain.PushType;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.infrastructure.firebase.FakePushSender;
import com.lingring.infrastructure.firebase.FakePushSenderConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(FakePushSenderConfig.class)
class PushSendServiceTest extends ServiceIntegrationHelper {

    private static final Long ME = 1L;
    private static final Long OTHER = 2L;

    @Autowired
    private PushSendService pushSendService;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private FakePushSender fakePushSender;

    @BeforeEach
    void clearFake() {
        fakePushSender.clear();
    }

    @Nested
    @DisplayName("sendTest: 본인 기기로 테스트 발송")
    class SendTest {

        @Test
        @DisplayName("내 토큰 전부로 DAILY_REMINDER payload를 보내고 다른 유저 토큰으로는 보내지 않는다")
        void sendTest_sendsDailyReminderOnlyToMyTokens() {
            // given
            saveToken(ME, "my-ios");
            saveToken(ME, "my-android");
            saveToken(OTHER, "other-token");

            // when
            pushSendService.sendTest(ME);

            // then
            assertThat(fakePushSender.sentTokens()).containsExactlyInAnyOrder("my-ios", "my-android");
            assertThat(fakePushSender.sent())
                    .allSatisfy(push -> assertThat(push.message().type()).isEqualTo(PushType.DAILY_REMINDER));
        }

        @Test
        @DisplayName("등록된 토큰이 없으면 아무것도 보내지 않는다")
        void sendTest_whenNoTokens_sendsNothing() {
            // when
            pushSendService.sendTest(ME);

            // then
            assertThat(fakePushSender.sent()).isEmpty();
        }

        @Test
        @DisplayName("FCM이 무효로 응답한 토큰은 삭제하고 나머지는 유지한다")
        void sendTest_whenInvalidToken_deletesOnlyInvalid() {
            // given
            saveToken(ME, "valid-token");
            saveToken(ME, "stale-token");
            fakePushSender.markInvalid("stale-token");

            // when
            pushSendService.sendTest(ME);

            // then
            assertThat(deviceTokenRepository.findAll())
                    .extracting(DeviceToken::getToken)
                    .containsExactly("valid-token");
        }
    }

    private void saveToken(final Long userId, final String token) {
        deviceTokenRepository.save(DeviceToken.register(userId, token, Platform.IOS));
    }
}
