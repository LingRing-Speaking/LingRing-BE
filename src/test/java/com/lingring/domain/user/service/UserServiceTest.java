package com.lingring.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.dto.response.MeResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("getMe: 인증된 사용자 본인 정보 조회")
    class GetMe {

        @Test
        @DisplayName("존재하는 사용자 id로 조회하면 id와 nickname을 반환한다")
        void getMe_whenUserExists_returnsResponse() {
            // given
            final User saved = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "kakao-test-sub-1", new Name("링링"), null)
            );

            // when
            final MeResponse response = userService.getMe(saved.getId());

            // then
            assertThat(response.id()).isEqualTo(saved.getId());
            assertThat(response.nickname()).isEqualTo("링링");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 id로 조회하면 INVALID_TOKEN 예외가 발생한다 (401)")
        void getMe_whenUserNotFound_throwsUnauthorized() {
            // given
            final Long missingId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> userService.getMe(missingId))
                    .isInstanceOf(UnauthorizedException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_TOKEN);
        }
    }
}
