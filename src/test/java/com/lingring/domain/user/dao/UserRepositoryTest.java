package com.lingring.domain.user.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.config.RepositoryTestHelper;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRepositoryTest extends RepositoryTestHelper {

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("findByProviderAndProviderUserId")
    class FindByProviderAndProviderUserId {

        @Test
        @DisplayName("provider와 providerUserId가 일치하는 User가 있으면 반환한다")
        void findByProviderAndProviderUserId_whenExists_returnsUser() {
            // given
            final User saved = userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "kakao-sub-1", new Name("링링"), null)
            );

            // when
            final Optional<User> found =
                    userRepository.findByProviderAndProviderUserId(Provider.KAKAO, "kakao-sub-1");

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("일치하는 User가 없으면 Optional.empty()를 반환한다")
        void findByProviderAndProviderUserId_whenNotExists_returnsEmpty() {
            // when
            final Optional<User> found =
                    userRepository.findByProviderAndProviderUserId(Provider.KAKAO, "missing-sub");

            // then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("providerUserId는 같지만 provider가 다르면 Optional.empty()를 반환한다")
        void findByProviderAndProviderUserId_whenSameSubDifferentProvider_returnsEmpty() {
            // given
            userRepository.save(
                    User.createFromOAuth(Provider.KAKAO, "shared-sub", new Name("링링"), null)
            );

            // when
            final Optional<User> found =
                    userRepository.findByProviderAndProviderUserId(Provider.APPLE, "shared-sub");

            // then
            assertThat(found).isEmpty();
        }
    }
}
