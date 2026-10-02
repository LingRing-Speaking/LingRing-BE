package com.lingring.domain.push.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.push.domain.DeviceToken;
import com.lingring.domain.push.domain.Platform;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.config.RepositoryTestHelper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;

class DeviceTokenRepositoryTest extends RepositoryTestHelper {

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("findDailyReminderTargets: 마케팅 푸시 동의 유저의 토큰 커서 조회")
    class FindDailyReminderTargets {

        @Test
        @DisplayName("수신 동의한 유저의 토큰만 id 오름차순으로 반환한다")
        void findDailyReminderTargets_returnsOnlyAgreedUsersTokens() {
            // given
            final User agreed = saveUser("동의유저", "kakao-agreed", true);
            final User notAgreed = saveUser("미동의유저", "kakao-not-agreed", false);
            saveToken(agreed, "agreed-1");
            saveToken(notAgreed, "not-agreed-1");
            saveToken(agreed, "agreed-2");

            // when
            final List<DeviceToken> targets = deviceTokenRepository.findDailyReminderTargets(0L, Limit.of(10));

            // then
            assertThat(targets)
                    .extracting(DeviceToken::getToken)
                    .containsExactly("agreed-1", "agreed-2");
        }

        @Test
        @DisplayName("lastId보다 큰 id만 limit 개수까지 반환한다")
        void findDailyReminderTargets_appliesCursorAndLimit() {
            // given
            final User agreed = saveUser("동의유저", "kakao-agreed", true);
            final DeviceToken first = saveToken(agreed, "token-1");
            saveToken(agreed, "token-2");
            saveToken(agreed, "token-3");
            saveToken(agreed, "token-4");

            // when
            final List<DeviceToken> targets = deviceTokenRepository.findDailyReminderTargets(
                    first.getId(),
                    Limit.of(2)
            );

            // then
            assertThat(targets)
                    .extracting(DeviceToken::getToken)
                    .containsExactly("token-2", "token-3");
        }
    }

    private User saveUser(final String nickname, final String providerUserId, final boolean marketingPushAgreed) {
        final User user = User.createFromOAuth(Provider.KAKAO, providerUserId, new Name(nickname), null);
        user.changeMarketingPushConsent(marketingPushAgreed, LocalDateTime.of(2026, 10, 2, 12, 0));
        return userRepository.save(user);
    }

    private DeviceToken saveToken(final User user, final String token) {
        return deviceTokenRepository.save(DeviceToken.register(user.getId(), token, Platform.ANDROID));
    }
}
