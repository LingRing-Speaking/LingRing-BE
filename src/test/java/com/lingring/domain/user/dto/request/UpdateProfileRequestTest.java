package com.lingring.domain.user.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UpdateProfileRequestTest {

    @Nested
    @DisplayName("hasNoChanges: 변경 항목이 하나도 없는지 판정")
    class HasNoChanges {

        @Test
        @DisplayName("nickname과 profileImageKey가 모두 null이면 true를 반환한다")
        void hasNoChanges_whenBothNull_returnsTrue() {
            // given
            final UpdateProfileRequest request = new UpdateProfileRequest(null, null);

            // when & then
            assertThat(request.hasNoChanges()).isTrue();
        }

        @Test
        @DisplayName("nickname과 profileImageKey가 모두 공백이면 true를 반환한다")
        void hasNoChanges_whenBothBlank_returnsTrue() {
            // given
            final UpdateProfileRequest request = new UpdateProfileRequest("   ", "   ");

            // when & then
            assertThat(request.hasNoChanges()).isTrue();
        }

        @Test
        @DisplayName("nickname만 값이 있으면 false를 반환한다")
        void hasNoChanges_whenOnlyNicknamePresent_returnsFalse() {
            // given
            final UpdateProfileRequest request = new UpdateProfileRequest("새이름", null);

            // when & then
            assertThat(request.hasNoChanges()).isFalse();
        }

        @Test
        @DisplayName("profileImageKey만 값이 있으면 false를 반환한다")
        void hasNoChanges_whenOnlyImageKeyPresent_returnsFalse() {
            // given
            final UpdateProfileRequest request = new UpdateProfileRequest(null, "profile-images/1/abc");

            // when & then
            assertThat(request.hasNoChanges()).isFalse();
        }

        @Test
        @DisplayName("둘 다 값이 있으면 false를 반환한다")
        void hasNoChanges_whenBothPresent_returnsFalse() {
            // given
            final UpdateProfileRequest request = new UpdateProfileRequest("새이름", "profile-images/1/abc");

            // when & then
            assertThat(request.hasNoChanges()).isFalse();
        }
    }
}
