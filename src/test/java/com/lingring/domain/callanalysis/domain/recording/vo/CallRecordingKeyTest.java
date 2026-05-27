package com.lingring.domain.callanalysis.domain.recording.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.callanalysis.exception.CallRecordingKeyForbiddenException;
import com.lingring.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CallRecordingKeyTest {

    @Nested
    @DisplayName("generateFor: callId/userId 기반 키 생성")
    class GenerateFor {

        @Test
        @DisplayName("callId/userId prefix와 UUID 형태로 키를 생성한다")
        void generateFor_whenValid_returnsPrefixedKey() {
            // given
            final Long callId = 7L;
            final Long userId = 42L;

            // when
            final CallRecordingKey key = CallRecordingKey.generateFor(callId, userId);

            // then
            assertThat(key.getValue()).startsWith("call-recordings/7/42/");
            assertThat(key.getValue()).hasSizeGreaterThan("call-recordings/7/42/".length());
        }

        @Test
        @DisplayName("같은 callId/userId에 대해 매번 다른 키를 생성한다")
        void generateFor_whenCalledTwice_returnsDifferentKeys() {
            // given
            final Long callId = 1L;
            final Long userId = 2L;

            // when
            final CallRecordingKey first = CallRecordingKey.generateFor(callId, userId);
            final CallRecordingKey second = CallRecordingKey.generateFor(callId, userId);

            // then
            assertThat(first.getValue()).isNotEqualTo(second.getValue());
        }

        @Test
        @DisplayName("callId가 null이면 NullPointerException이 발생한다")
        void generateFor_whenCallIdNull_throws() {
            // when & then
            assertThatThrownBy(() -> CallRecordingKey.generateFor(null, 1L))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("userId가 null이면 NullPointerException이 발생한다")
        void generateFor_whenUserIdNull_throws() {
            // when & then
            assertThatThrownBy(() -> CallRecordingKey.generateFor(1L, null))
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
            final String raw = "call-recordings/1/2/abc";

            // when
            final CallRecordingKey key = CallRecordingKey.from(raw);

            // then
            assertThat(key.getValue()).isEqualTo(raw);
        }

        @Test
        @DisplayName("null이면 NullPointerException이 발생한다")
        void from_whenNull_throws() {
            // when & then
            assertThatThrownBy(() -> CallRecordingKey.from(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("requireOwnedBy: callId+userId 본인 소유 검증")
    class RequireOwnedBy {

        @Test
        @DisplayName("callId와 userId가 모두 일치하면 통과한다")
        void requireOwnedBy_whenOwned_passes() {
            // given
            final CallRecordingKey key = CallRecordingKey.from("call-recordings/7/42/abc");

            // when & then
            assertThatCode(() -> key.requireOwnedBy(7L, 42L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("다른 callId면 CallRecordingKeyForbiddenException이 발생한다")
        void requireOwnedBy_whenForeignCallId_throws() {
            // given
            final CallRecordingKey key = CallRecordingKey.from("call-recordings/9999/42/abc");

            // when & then
            assertThatThrownBy(() -> key.requireOwnedBy(7L, 42L))
                    .isInstanceOf(CallRecordingKeyForbiddenException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CALL_RECORDING_KEY_FORBIDDEN);
        }

        @Test
        @DisplayName("다른 userId면 CallRecordingKeyForbiddenException이 발생한다")
        void requireOwnedBy_whenForeignUserId_throws() {
            // given
            final CallRecordingKey key = CallRecordingKey.from("call-recordings/7/9999/abc");

            // when & then
            assertThatThrownBy(() -> key.requireOwnedBy(7L, 42L))
                    .isInstanceOf(CallRecordingKeyForbiddenException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CALL_RECORDING_KEY_FORBIDDEN);
        }

        @Test
        @DisplayName("prefix 형식이 아예 다르면 CallRecordingKeyForbiddenException이 발생한다")
        void requireOwnedBy_whenInvalidFormat_throws() {
            // given
            final CallRecordingKey key = CallRecordingKey.from("invalid-format");

            // when & then
            assertThatThrownBy(() -> key.requireOwnedBy(7L, 42L))
                    .isInstanceOf(CallRecordingKeyForbiddenException.class);
        }

        @Test
        @DisplayName("동일 id로 시작하지만 자릿수가 다르면 차단한다 (예: '7' prefix가 '70'에 부분 매칭되지 않는다)")
        void requireOwnedBy_whenIdPrefixCollision_throws() {
            // given — callId=70 의 키지만 callId=7 로 검증 시도
            final CallRecordingKey key = CallRecordingKey.from("call-recordings/70/42/abc");

            // when & then
            assertThatThrownBy(() -> key.requireOwnedBy(7L, 42L))
                    .isInstanceOf(CallRecordingKeyForbiddenException.class);
        }

        @Test
        @DisplayName("callId가 null이면 NullPointerException이 발생한다")
        void requireOwnedBy_whenCallIdNull_throws() {
            // given
            final CallRecordingKey key = CallRecordingKey.from("call-recordings/7/42/abc");

            // when & then
            assertThatThrownBy(() -> key.requireOwnedBy(null, 42L))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("userId가 null이면 NullPointerException이 발생한다")
        void requireOwnedBy_whenUserIdNull_throws() {
            // given
            final CallRecordingKey key = CallRecordingKey.from("call-recordings/7/42/abc");

            // when & then
            assertThatThrownBy(() -> key.requireOwnedBy(7L, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
