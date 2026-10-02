package com.lingring.domain.push.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DeviceTokenTest {

    @Nested
    @DisplayName("register: 디바이스 토큰 생성")
    class Register {

        @Test
        @DisplayName("소유자·토큰·플랫폼을 그대로 보관한다")
        void register_whenValid_holdsValues() {
            // when
            final DeviceToken deviceToken = DeviceToken.register(1L, "fcm-token", Platform.IOS);

            // then
            assertThat(deviceToken.getUserId()).isEqualTo(1L);
            assertThat(deviceToken.getToken()).isEqualTo("fcm-token");
            assertThat(deviceToken.getPlatform()).isEqualTo(Platform.IOS);
        }

        @Test
        @DisplayName("토큰이 null이면 NPE")
        void register_whenTokenNull_throwsNpe() {
            // when & then
            assertThatThrownBy(() -> DeviceToken.register(1L, null, Platform.IOS))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("reassign: 소유자 이전")
    class Reassign {

        @Test
        @DisplayName("다른 유저로 소유자와 플랫폼을 옮긴다")
        void reassign_whenOtherUser_changesOwnerAndPlatform() {
            // given
            final DeviceToken deviceToken = DeviceToken.register(1L, "fcm-token", Platform.IOS);

            // when
            deviceToken.reassign(2L, Platform.ANDROID);

            // then
            assertThat(deviceToken.getUserId()).isEqualTo(2L);
            assertThat(deviceToken.getPlatform()).isEqualTo(Platform.ANDROID);
        }
    }
}
