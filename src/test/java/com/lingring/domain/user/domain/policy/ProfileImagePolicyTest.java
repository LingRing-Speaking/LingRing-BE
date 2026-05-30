package com.lingring.domain.user.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProfileImagePolicyTest {

    private static final ProfileImagePolicy POLICY = new ProfileImagePolicy("image/", 5_242_880L);

    @Nested
    @DisplayName("requireAcceptable: 업로드 입력 검증")
    class RequireAcceptable {

        @Test
        @DisplayName("contentType이 image/로 시작하고 크기가 1~max 사이면 통과한다")
        void requireAcceptable_whenValid_passes() {
            // when & then
            assertThatCode(() -> POLICY.requireAcceptable("image/png", 1024L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("크기가 정확히 max bytes면 통과한다")
        void requireAcceptable_whenExactlyMax_passes() {
            // when & then
            assertThatCode(() -> POLICY.requireAcceptable("image/png", 5_242_880L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("contentType이 null이면 INVALID_IMAGE_CONTENT_TYPE 예외가 발생한다")
        void requireAcceptable_whenContentTypeNull_throws() {
            // when & then
            assertThatThrownBy(() -> POLICY.requireAcceptable(null, 1024L))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_IMAGE_CONTENT_TYPE);
        }

        @Test
        @DisplayName("contentType이 image/로 시작하지 않으면 INVALID_IMAGE_CONTENT_TYPE 예외가 발생한다")
        void requireAcceptable_whenNotImage_throws() {
            // when & then
            assertThatThrownBy(() -> POLICY.requireAcceptable("application/pdf", 1024L))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_IMAGE_CONTENT_TYPE);
        }

        @Test
        @DisplayName("contentLength가 null이면 IMAGE_TOO_LARGE 예외가 발생한다")
        void requireAcceptable_whenContentLengthNull_throws() {
            // when & then
            assertThatThrownBy(() -> POLICY.requireAcceptable("image/png", null))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.IMAGE_TOO_LARGE);
        }

        @Test
        @DisplayName("contentLength가 0 이하면 IMAGE_TOO_LARGE 예외가 발생한다")
        void requireAcceptable_whenContentLengthNonPositive_throws() {
            // when & then
            assertThatThrownBy(() -> POLICY.requireAcceptable("image/png", 0L))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.IMAGE_TOO_LARGE);
        }

        @Test
        @DisplayName("contentLength가 max를 초과하면 IMAGE_TOO_LARGE 예외가 발생한다")
        void requireAcceptable_whenContentLengthOverMax_throws() {
            // when & then
            assertThatThrownBy(() -> POLICY.requireAcceptable("image/png", 5_242_881L))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.IMAGE_TOO_LARGE);
        }
    }
}
