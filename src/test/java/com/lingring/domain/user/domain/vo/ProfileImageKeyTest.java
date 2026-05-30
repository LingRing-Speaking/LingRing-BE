package com.lingring.domain.user.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProfileImageKeyTest {

    @Nested
    @DisplayName("generateFor: userId 기반 키 생성")
    class GenerateFor {

        @Test
        @DisplayName("userId prefix와 UUID 형태로 키를 생성한다")
        void generateFor_whenValid_returnsPrefixedKey() {
            // given
            final Long userId = 42L;

            // when
            final ProfileImageKey key = ProfileImageKey.generateFor(userId);

            // then
            assertThat(key.getValue()).startsWith("profile-images/42/");
            assertThat(key.getValue()).hasSizeGreaterThan("profile-images/42/".length());
        }

        @Test
        @DisplayName("같은 userId에 대해 매번 다른 키를 생성한다")
        void generateFor_whenCalledTwice_returnsDifferentKeys() {
            // given
            final Long userId = 1L;

            // when
            final ProfileImageKey first = ProfileImageKey.generateFor(userId);
            final ProfileImageKey second = ProfileImageKey.generateFor(userId);

            // then
            assertThat(first.getValue()).isNotEqualTo(second.getValue());
        }

        @Test
        @DisplayName("userId가 null이면 NullPointerException이 발생한다")
        void generateFor_whenNull_throws() {
            // when & then
            assertThatThrownBy(() -> ProfileImageKey.generateFor(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("from: 외부 입력 키 wrapping")
    class From {

        @Test
        @DisplayName("문자열을 그대로 wrapping한다")
        void from_whenValid_returnsKey() {
            // given
            final String raw = "profile-images/1/abc";

            // when
            final ProfileImageKey key = ProfileImageKey.from(raw);

            // then
            assertThat(key.getValue()).isEqualTo(raw);
        }

        @Test
        @DisplayName("null이면 NullPointerException이 발생한다")
        void from_whenNull_throws() {
            // when & then
            assertThatThrownBy(() -> ProfileImageKey.from(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("requireOwnedBy: 본인 소유 검증")
    class RequireOwnedBy {

        @Test
        @DisplayName("본인 prefix가 일치하면 통과한다")
        void requireOwnedBy_whenOwned_passes() {
            // given
            final ProfileImageKey key = ProfileImageKey.from("profile-images/1/abc");

            // when & then
            assertThatCode(() -> key.requireOwnedBy(1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("다른 사용자의 prefix면 ForbiddenException이 발생한다")
        void requireOwnedBy_whenForeign_throws() {
            // given
            final ProfileImageKey key = ProfileImageKey.from("profile-images/9999/x");

            // when & then
            assertThatThrownBy(() -> key.requireOwnedBy(1L))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PROFILE_IMAGE_KEY_FORBIDDEN);
        }

        @Test
        @DisplayName("prefix 형식이 아예 다르면 ForbiddenException이 발생한다")
        void requireOwnedBy_whenInvalidFormat_throws() {
            // given
            final ProfileImageKey key = ProfileImageKey.from("invalid-format");

            // when & then
            assertThatThrownBy(() -> key.requireOwnedBy(1L))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PROFILE_IMAGE_KEY_FORBIDDEN);
        }

        @Test
        @DisplayName("동일 userId 시작이지만 자릿수가 다르면 차단한다 (예: '1' prefix가 '12'에 부분 매칭되지 않는다)")
        void requireOwnedBy_whenIdPrefixCollision_throws() {
            // given — userId=12 의 키지만 userId=1 으로 검증 시도
            final ProfileImageKey key = ProfileImageKey.from("profile-images/12/abc");

            // when & then
            assertThatThrownBy(() -> key.requireOwnedBy(1L))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PROFILE_IMAGE_KEY_FORBIDDEN);
        }

        @Test
        @DisplayName("userId가 null이면 NullPointerException이 발생한다")
        void requireOwnedBy_whenNull_throws() {
            // given
            final ProfileImageKey key = ProfileImageKey.from("profile-images/1/abc");

            // when & then
            assertThatThrownBy(() -> key.requireOwnedBy(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
