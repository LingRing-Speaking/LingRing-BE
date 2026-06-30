package com.lingring.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.domain.vo.ProfileImage;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Provider PROVIDER = Provider.KAKAO;
    private static final String PROVIDER_USER_ID = "1234567890";
    private static final Name NAME = new Name("링링");
    private static final String PROFILE_IMAGE_URL = "https://cdn.example.com/img.png";

    @Nested
    @DisplayName("createFromOAuth: OAuth 가입 팩토리")
    class CreateFromOAuth {

        @Test
        @DisplayName("모든 값이 유효하면 User를 생성한다")
        void createFromOAuth_whenValidInput_returnsUser() {
            // when
            final User user = User.createFromOAuth(PROVIDER, PROVIDER_USER_ID, NAME, PROFILE_IMAGE_URL);

            // then
            assertThat(user.getProvider()).isEqualTo(PROVIDER);
            assertThat(user.getProviderUserId()).isEqualTo(PROVIDER_USER_ID);
            assertThat(user.getName()).isEqualTo(NAME);
            assertThat(user.getProfileImage()).isEqualTo(new ProfileImage(PROFILE_IMAGE_URL));
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
                    () -> User.createFromOAuth(null, PROVIDER_USER_ID, NAME, PROFILE_IMAGE_URL)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("providerUserId가 null이면 NullPointerException이 발생한다")
        void createFromOAuth_whenProviderUserIdNull_throws() {
            // when & then
            assertThatThrownBy(
                    () -> User.createFromOAuth(PROVIDER, null, NAME, PROFILE_IMAGE_URL)
            ).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("name이 null이면 NullPointerException이 발생한다")
        void createFromOAuth_whenNameNull_throws() {
            // when & then
            assertThatThrownBy(
                    () -> User.createFromOAuth(PROVIDER, PROVIDER_USER_ID, null, PROFILE_IMAGE_URL)
            ).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("changeName: 닉네임 변경")
    class ChangeName {

        @Test
        @DisplayName("새 Name으로 변경되면 getName이 새 값을 반환한다")
        void changeName_whenValid_updates() {
            // given
            final User user = User.createFromOAuth(PROVIDER, PROVIDER_USER_ID, NAME, PROFILE_IMAGE_URL);
            final Name newName = new Name("새이름");

            // when
            user.changeName(newName);

            // then
            assertThat(user.getName()).isEqualTo(newName);
        }

        @Test
        @DisplayName("null을 전달하면 NullPointerException이 발생한다")
        void changeName_whenNull_throws() {
            // given
            final User user = User.createFromOAuth(PROVIDER, PROVIDER_USER_ID, NAME, PROFILE_IMAGE_URL);

            // when & then
            assertThatThrownBy(() -> user.changeName(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("changeProfileImage: 프로필 이미지 변경")
    class ChangeProfileImage {

        @Test
        @DisplayName("새 ProfileImage로 변경되면 getProfileImage가 새 값을 반환한다")
        void changeProfileImage_whenValid_updates() {
            // given
            final User user = User.createFromOAuth(PROVIDER, PROVIDER_USER_ID, NAME, PROFILE_IMAGE_URL);
            final ProfileImage newProfileImage = new ProfileImage("https://cdn.example.com/new.png");

            // when
            user.changeProfileImage(newProfileImage);

            // then
            assertThat(user.getProfileImage()).isEqualTo(newProfileImage);
        }

        @Test
        @DisplayName("null을 전달하면 NullPointerException이 발생한다")
        void changeProfileImage_whenNull_throws() {
            // given
            final User user = User.createFromOAuth(PROVIDER, PROVIDER_USER_ID, NAME, PROFILE_IMAGE_URL);

            // when & then
            assertThatThrownBy(() -> user.changeProfileImage(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("동의 상태: requiresOnboarding / agreedTermsVersion")
    class Agreement {

        @Test
        @DisplayName("동의 전에는 requiresOnboarding=true, agreedTermsVersion=null")
        void beforeAgreement_requiresOnboardingAndNullVersion() {
            // given
            final User user = User.createFromOAuth(PROVIDER, PROVIDER_USER_ID, NAME, PROFILE_IMAGE_URL);

            // when & then
            assertThat(user.requiresOnboarding()).isTrue();
            assertThat(user.agreedTermsVersion()).isNull();
        }

        @Test
        @DisplayName("동의 후에는 requiresOnboarding=false, agreedTermsVersion=동의한 버전")
        void afterAgreement_returnsAgreedVersion() {
            // given
            final User user = User.createFromOAuth(PROVIDER, PROVIDER_USER_ID, NAME, PROFILE_IMAGE_URL);

            // when
            user.markAgreed("2026-06-30", LocalDateTime.of(2026, 6, 30, 10, 0));

            // then
            assertThat(user.requiresOnboarding()).isFalse();
            assertThat(user.agreedTermsVersion()).isEqualTo("2026-06-30");
        }
    }
}
