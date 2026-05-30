package com.lingring.domain.user.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProfileImageTest {

    @Nested
    @DisplayName("생성자: ProfileImage 생성")
    class Constructor {

        @Test
        @DisplayName("유효한 URL이면 value를 보관한다")
        void constructor_whenValid_setsValue() {
            // given
            final String url = "https://cdn.example.com/img.png";

            // when
            final ProfileImage profileImage = new ProfileImage(url);

            // then
            assertThat(profileImage.getValue()).isEqualTo(url);
        }

        @Test
        @DisplayName("null을 전달하면 NullPointerException이 발생한다")
        void constructor_whenNull_throws() {
            // when & then
            assertThatThrownBy(() -> new ProfileImage(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("빈 문자열을 전달하면 InvalidValueException이 발생한다")
        void constructor_whenBlank_throws() {
            // when & then
            assertThatThrownBy(() -> new ProfileImage("   "))
                    .isInstanceOf(InvalidValueException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("500자를 초과하면 InvalidValueException이 발생한다")
        void constructor_whenTooLong_throws() {
            // given
            final String tooLong = "a".repeat(501);

            // when & then
            assertThatThrownBy(() -> new ProfileImage(tooLong))
                    .isInstanceOf(InvalidValueException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("정확히 500자면 정상 생성된다")
        void constructor_whenExactlyMaxLength_succeeds() {
            // given
            final String exact = "a".repeat(500);

            // when
            final ProfileImage profileImage = new ProfileImage(exact);

            // then
            assertThat(profileImage.getValue()).isEqualTo(exact);
        }
    }

    @Nested
    @DisplayName("fromNullable: null URL을 ProfileImage로 brige")
    class FromNullable {

        @Test
        @DisplayName("null URL이면 null을 반환한다")
        void fromNullable_whenNull_returnsNull() {
            // when & then
            assertThat(ProfileImage.fromNullable(null)).isNull();
        }

        @Test
        @DisplayName("유효한 URL이면 ProfileImage를 반환한다")
        void fromNullable_whenValid_returnsProfileImage() {
            // given
            final String url = "https://cdn.example.com/img.png";

            // when
            final ProfileImage profileImage = ProfileImage.fromNullable(url);

            // then
            assertThat(profileImage).isNotNull();
            assertThat(profileImage.getValue()).isEqualTo(url);
        }

        @Test
        @DisplayName("blank URL이면 InvalidValueException이 발생한다 (생성자 검증 위임)")
        void fromNullable_whenBlank_throws() {
            // when & then
            assertThatThrownBy(() -> ProfileImage.fromNullable("   "))
                    .isInstanceOf(InvalidValueException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Nested
    @DisplayName("equals/hashCode")
    class Equality {

        @Test
        @DisplayName("같은 URL을 가진 두 객체는 동등하다")
        void equals_whenSameValue_returnsTrue() {
            // given
            final ProfileImage a = new ProfileImage("https://cdn.example.com/img.png");
            final ProfileImage b = new ProfileImage("https://cdn.example.com/img.png");

            // when & then
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }
    }
}
