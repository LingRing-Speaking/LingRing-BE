package com.lingring.domain.push.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.push.dao.DeviceTokenRepository;
import com.lingring.domain.push.domain.DeviceToken;
import com.lingring.domain.push.domain.Platform;
import com.lingring.domain.push.dto.request.DeviceTokenRegisterRequest;
import com.lingring.domain.push.dto.request.DeviceTokenUnregisterRequest;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DeviceTokenServiceTest extends ServiceIntegrationHelper {

    private static final Long ME = 1L;
    private static final Long OTHER = 2L;

    @Autowired
    private DeviceTokenService deviceTokenService;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Nested
    @DisplayName("register: 디바이스 토큰 등록")
    class Register {

        @Test
        @DisplayName("처음 보는 토큰이면 요청한 유저 소유로 저장한다")
        void register_whenNewToken_savesToken() {
            // when
            deviceTokenService.register(ME, new DeviceTokenRegisterRequest("token-a", "ios"));

            // then
            final DeviceToken saved = deviceTokenRepository.findByToken("token-a").orElseThrow();
            assertThat(saved.getUserId()).isEqualTo(ME);
            assertThat(saved.getPlatform()).isEqualTo(Platform.IOS);
        }

        @Test
        @DisplayName("같은 요청을 반복해도 행이 하나만 남는다")
        void register_whenRepeated_keepsSingleRow() {
            // given
            deviceTokenService.register(ME, new DeviceTokenRegisterRequest("token-a", "ios"));

            // when
            deviceTokenService.register(ME, new DeviceTokenRegisterRequest("token-a", "ios"));

            // then
            assertThat(deviceTokenRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("다른 유저 소유 토큰이면 요청한 유저로 소유자를 옮긴다")
        void register_whenOwnedByOther_reassignsOwner() {
            // given
            deviceTokenRepository.save(DeviceToken.register(OTHER, "token-a", Platform.IOS));

            // when
            deviceTokenService.register(ME, new DeviceTokenRegisterRequest("token-a", "ios"));

            // then
            assertThat(deviceTokenRepository.findAll())
                    .singleElement()
                    .extracting(DeviceToken::getUserId)
                    .isEqualTo(ME);
        }

        @Test
        @DisplayName("한 유저가 여러 기기의 토큰을 가질 수 있다")
        void register_whenMultipleDevices_keepsAll() {
            // when
            deviceTokenService.register(ME, new DeviceTokenRegisterRequest("token-ios", "ios"));
            deviceTokenService.register(ME, new DeviceTokenRegisterRequest("token-android", "android"));

            // then
            assertThat(deviceTokenRepository.findAll())
                    .extracting(DeviceToken::getToken)
                    .containsExactlyInAnyOrder("token-ios", "token-android");
        }

        @Test
        @DisplayName("지원하지 않는 platform이면 BadRequestException이 발생하고 저장하지 않는다")
        void register_whenUnknownPlatform_throwsBadRequest() {
            // when & then
            assertThatThrownBy(() -> deviceTokenService.register(ME, new DeviceTokenRegisterRequest("token-a", "web")))
                    .isInstanceOf(BadRequestException.class);
            assertThat(deviceTokenRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("unregister: 디바이스 토큰 해제")
    class Unregister {

        @Test
        @DisplayName("내 소유 토큰이면 삭제한다")
        void unregister_whenOwnedByMe_deletesToken() {
            // given
            deviceTokenRepository.save(DeviceToken.register(ME, "token-a", Platform.IOS));

            // when
            deviceTokenService.unregister(ME, new DeviceTokenUnregisterRequest("token-a"));

            // then
            assertThat(deviceTokenRepository.findByToken("token-a")).isEmpty();
        }

        @Test
        @DisplayName("다른 유저 소유 토큰이면 삭제하지 않고 예외 없이 끝난다")
        void unregister_whenOwnedByOther_keepsToken() {
            // given
            deviceTokenRepository.save(DeviceToken.register(OTHER, "token-a", Platform.IOS));

            // when
            deviceTokenService.unregister(ME, new DeviceTokenUnregisterRequest("token-a"));

            // then
            assertThat(deviceTokenRepository.findByToken("token-a")).isPresent();
        }

        @Test
        @DisplayName("없는 토큰이어도 예외 없이 끝난다")
        void unregister_whenTokenMissing_doesNothing() {
            // when
            deviceTokenService.unregister(ME, new DeviceTokenUnregisterRequest("unknown-token"));

            // then
            assertThat(deviceTokenRepository.count()).isZero();
        }
    }
}
