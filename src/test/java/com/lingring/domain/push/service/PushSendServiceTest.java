package com.lingring.domain.push.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.push.dao.DeviceTokenRepository;
import com.lingring.domain.push.domain.DeviceToken;
import com.lingring.domain.push.domain.Platform;
import com.lingring.domain.push.domain.PushType;
import com.lingring.domain.push.domain.policy.DailyReminderPolicy;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.util.FixedDateTimeProvider;
import com.lingring.infrastructure.firebase.FakePushSender;
import com.lingring.infrastructure.firebase.FakePushSenderConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import({FakePushSenderConfig.class, PushSendServiceTest.FixedDateTimeProviderConfig.class})
class PushSendServiceTest extends ServiceIntegrationHelper {

    private static final Long ME = 1L;
    private static final Long OTHER = 2L;
    private static final LocalDateTime REMINDER_TIME = LocalDateTime.of(2026, 10, 2, 20, 0);

    @Autowired
    private PushSendService pushSendService;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FakePushSender fakePushSender;

    @Autowired
    private FixedDateTimeProvider fixedDateTimeProvider;

    @TestConfiguration
    static class FixedDateTimeProviderConfig {

        @Bean
        @Primary
        FixedDateTimeProvider dateTimeProvider() {
            return new FixedDateTimeProvider(REMINDER_TIME);
        }
    }

    @BeforeEach
    void resetFakes() {
        fakePushSender.clear();
        fixedDateTimeProvider.setFixedTime(REMINDER_TIME);
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

    @Nested
    @DisplayName("sendDailyReminder: 20시 리마인더 발송")
    class SendDailyReminder {

        @Test
        @DisplayName("수신 동의한 유저의 토큰 전부로만 보낸다")
        void sendDailyReminder_sendsOnlyToAgreedUsers() {
            // given
            final User agreed = saveUser("동의유저", "kakao-agreed", true);
            final User notAgreed = saveUser("미동의유저", "kakao-not-agreed", false);
            saveToken(agreed.getId(), "agreed-ios");
            saveToken(agreed.getId(), "agreed-android");
            saveToken(notAgreed.getId(), "not-agreed-token");

            // when
            pushSendService.sendDailyReminder();

            // then
            assertThat(fakePushSender.sentTokens()).containsExactlyInAnyOrder("agreed-ios", "agreed-android");
        }

        @Test
        @DisplayName("대상이 한 번에 조회하는 개수보다 많아도 전부 한 번씩 보낸다")
        void sendDailyReminder_whenMoreThanChunk_sendsAllOnce() {
            // given
            final User agreed = saveUser("동의유저", "kakao-agreed", true);
            final int total = DailyReminderPolicy.CHUNK_SIZE + 1;
            deviceTokenRepository.saveAll(IntStream.range(0, total)
                    .mapToObj(i -> DeviceToken.register(agreed.getId(), "token-" + i, Platform.ANDROID))
                    .toList());

            // when
            pushSendService.sendDailyReminder();

            // then
            final List<String> sentTokens = fakePushSender.sentTokens();
            assertThat(sentTokens).hasSize(total);
            assertThat(sentTokens).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("21시가 지났으면 아무것도 보내지 않는다")
        void sendDailyReminder_whenAfterDeadline_sendsNothing() {
            // given
            final User agreed = saveUser("동의유저", "kakao-agreed", true);
            saveToken(agreed.getId(), "agreed-token");
            fixedDateTimeProvider.setFixedTime(REMINDER_TIME.withHour(21));

            // when
            pushSendService.sendDailyReminder();

            // then
            assertThat(fakePushSender.sent()).isEmpty();
        }

        @Test
        @DisplayName("FCM이 무효로 응답한 토큰은 삭제한다")
        void sendDailyReminder_whenInvalidToken_deletesIt() {
            // given
            final User agreed = saveUser("동의유저", "kakao-agreed", true);
            saveToken(agreed.getId(), "valid-token");
            saveToken(agreed.getId(), "stale-token");
            fakePushSender.markInvalid("stale-token");

            // when
            pushSendService.sendDailyReminder();

            // then
            assertThat(deviceTokenRepository.findAll())
                    .extracting(DeviceToken::getToken)
                    .containsExactly("valid-token");
        }
    }

    private User saveUser(
            final String nickname,
            final String providerUserId,
            final boolean marketingPushAgreed
    ) {
        final User user = User.createFromOAuth(Provider.KAKAO, providerUserId, new Name(nickname), null);
        user.changeMarketingPushConsent(marketingPushAgreed, REMINDER_TIME.minusDays(1));
        return userRepository.save(user);
    }

    private void saveToken(final Long userId, final String token) {
        deviceTokenRepository.save(DeviceToken.register(userId, token, Platform.IOS));
    }
}
