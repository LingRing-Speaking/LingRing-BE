package com.lingring.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.dto.response.UserMyResponse;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
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
    @DisplayName("getMy: 사용자 본인 정보 조회")
    class GetMy {

        @Test
        @DisplayName("존재하는 사용자 id로 조회하면 id와 이름을 반환한다")
        void getMy_whenUserExists_returnsResponse() {
            // given
            final User saved = userRepository.save(User.create(new Name("링링"), null));

            // when
            final UserMyResponse response = userService.getMy(saved.getId());

            // then
            assertThat(response.id()).isEqualTo(saved.getId());
            assertThat(response.name()).isEqualTo("링링");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 id로 조회하면 USER_NOT_FOUND 예외가 발생한다")
        void getMy_whenUserNotFound_throwsNotFoundException() {
            // given
            final Long missingId = 9_999_999L;

            // when & then
            assertThatThrownBy(() -> userService.getMy(missingId))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }
}
