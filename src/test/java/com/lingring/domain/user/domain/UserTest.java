package com.lingring.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.domain.vo.Name;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Provider PROVIDER = Provider.KAKAO;
    private static final String PROVIDER_USER_ID = "1234567890";
    private static final Name NAME = new Name("링링");
    private static final String PROFILE_IMAGE = "https://cdn.example.com/img.png";

    @Nested
    @DisplayName("createFromOAuth: OAuth 가입 팩토리")
    class CreateFromOAuth {

        @Test
        @DisplayName("모든 값이 유효하면 User를 생성한다")
        void createFromOAuth_whenValidInput_returnsUser() {
            // when
            final User user = User.createFromOAuth(PROVIDER, PROVIDER_USER_ID, NAME, PROFILE_IMAGE);

            // then
            assertThat(user.getProvider()).isEqualTo(PROVIDER);
            assertThat(user.getProviderUserId()).isEqualTo(PROVIDER_USER_ID);
            assertThat(user.getName()).isEqualTo(NAME);
            assertThat(user.getProfileImage()).isEqualTo(PROFILE_IMAGE);
        }

        @Test
        @DisplayName("프로필 이미지가 null이어도 User를 생성한다")
        void createFromOAuth_whenProfileImageNull_returnsUser() {
            // when
            final User user = User.createFromOAuth(PROVIDER, PROVIDER_USER_ID, NAME, null);

            // then
            assertThat(user.getProfileImage()).isNull();
        }

        @Test
        @DisplayName("provider가 null이면 NullPointerException이 발생한다")
        void createFromOAuth_whenProviderNull_throws() {
            // when & then
            assertThatThrownBy(
                    () -> User.createFromOAuth(null, PROVIDER_USER_ID, NAME, PROFILE_IMAGE)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("providerUserId가 null이면 NullPointerException이 발생한다")
        void createFromOAuth_whenProviderUserIdNull_throws() {
            // when & then
            assertThatThrownBy(
                    () -> User.createFromOAuth(PROVIDER, null, NAME, PROFILE_IMAGE)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("name이 null이면 NullPointerException이 발생한다")
        void createFromOAuth_whenNameNull_throws() {
            // when & then
            assertThatThrownBy(
                    () -> User.createFromOAuth(PROVIDER, PROVIDER_USER_ID, null, PROFILE_IMAGE)
            ).isInstanceOf(NullPointerException.class);
        }
    }
}
