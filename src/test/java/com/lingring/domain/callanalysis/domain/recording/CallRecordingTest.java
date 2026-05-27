package com.lingring.domain.callanalysis.domain.recording;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CallRecordingTest {

    @Nested
    @DisplayName("upload: 신규 녹음 생성 팩토리")
    class Upload {

        @Test
        @DisplayName("필드를 그대로 보관하고 status=UPLOADED로 시작한다")
        void upload_whenValid_createsUploadedRecording() {
            // when
            final CallRecording recording = CallRecording.upload(1L, 42L, "call-recordings/1/42/abc", "audio/m4a");

            // then
            assertThat(recording.getCallId()).isEqualTo(1L);
            assertThat(recording.getUserId()).isEqualTo(42L);
            assertThat(recording.getRecordingKey()).isEqualTo("call-recordings/1/42/abc");
            assertThat(recording.getContentType()).isEqualTo("audio/m4a");
            assertThat(recording.getStatus()).isEqualTo(CallRecordingStatus.UPLOADED);
        }

        @Test
        @DisplayName("callId가 null이면 NullPointerException이 발생한다")
        void upload_whenCallIdNull_throws() {
            // when & then
            assertThatThrownBy(() -> CallRecording.upload(null, 42L, "k", "audio/m4a"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("userId가 null이면 NullPointerException이 발생한다")
        void upload_whenUserIdNull_throws() {
            // when & then
            assertThatThrownBy(() -> CallRecording.upload(1L, null, "k", "audio/m4a"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("recordingKey가 null이면 NullPointerException이 발생한다")
        void upload_whenRecordingKeyNull_throws() {
            // when & then
            assertThatThrownBy(() -> CallRecording.upload(1L, 42L, null, "audio/m4a"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("contentType이 null이면 NullPointerException이 발생한다")
        void upload_whenContentTypeNull_throws() {
            // when & then
            assertThatThrownBy(() -> CallRecording.upload(1L, 42L, "k", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
