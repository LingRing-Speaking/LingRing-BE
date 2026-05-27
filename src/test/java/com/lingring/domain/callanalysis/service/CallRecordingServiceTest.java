package com.lingring.domain.callanalysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.callanalysis.dao.CallRecordingRepository;
import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.callanalysis.domain.CallRecording;
import com.lingring.domain.callanalysis.domain.CallRecordingStatus;
import com.lingring.domain.callanalysis.dto.request.CallRecordingCreateRequest;
import com.lingring.domain.callanalysis.dto.request.CallRecordingPresignedUrlRequest;
import com.lingring.domain.callanalysis.dto.response.CallRecordingCreateResponse;
import com.lingring.domain.callanalysis.dto.response.CallRecordingPresignedUrlResponse;
import com.lingring.domain.call.exception.CallActiveException;
import com.lingring.domain.call.exception.CallNotFoundException;
import com.lingring.domain.call.exception.CallParticipantMismatchException;
import com.lingring.domain.callanalysis.exception.CallRecordingKeyForbiddenException;
import com.lingring.domain.callanalysis.exception.CallRecordingS3MissingException;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import(CallRecordingServiceTest.FakeStorageConfig.class)
class CallRecordingServiceTest extends ServiceIntegrationHelper {

    private static final LocalDateTime ENDED_AT = LocalDateTime.of(2026, 5, 20, 10, 0);
    private static final LocalDateTime STARTED_AT = ENDED_AT.minusMinutes(5);

    @Autowired
    private CallRecordingService callRecordingService;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private CallRecordingRepository callRecordingRepository;

    @Autowired
    private FakeCallRecordingStorage storage;

    @BeforeEach
    void clearStorage() {
        storage.reset();
    }

    @TestConfiguration
    static class FakeStorageConfig {

        @Bean
        @Primary
        FakeCallRecordingStorage callRecordingStorage() {
            return new FakeCallRecordingStorage();
        }
    }

    @Nested
    @DisplayName("createPresignedUrl: 업로드 URL 발급")
    class CreatePresignedUrl {

        @Test
        @DisplayName("정상 요청 시 url과 키를 반환하고 키는 callId/userId 기반이다")
        void createPresignedUrl_whenValid_returnsUrlAndKey() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            final CallRecordingPresignedUrlRequest request =
                    new CallRecordingPresignedUrlRequest("audio/m4a", 1024L);

            // when
            final CallRecordingPresignedUrlResponse response =
                    callRecordingService.createPresignedUrl(call.getId(), 1L, request);

            // then
            assertThat(response.url()).contains(response.key());
            assertThat(response.key()).startsWith("call-recordings/%d/1/".formatted(call.getId()));
        }

        @Test
        @DisplayName("존재하지 않는 callId면 CallNotFoundException")
        void createPresignedUrl_whenCallMissing_throws() {
            // given
            final CallRecordingPresignedUrlRequest request =
                    new CallRecordingPresignedUrlRequest("audio/m4a", 1024L);

            // when & then
            assertThatThrownBy(() -> callRecordingService.createPresignedUrl(9999L, 1L, request))
                    .isInstanceOf(CallNotFoundException.class);
        }

        @Test
        @DisplayName("참여자가 아니면 CallParticipantMismatchException")
        void createPresignedUrl_whenNotParticipant_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            final CallRecordingPresignedUrlRequest request =
                    new CallRecordingPresignedUrlRequest("audio/m4a", 1024L);

            // when & then
            assertThatThrownBy(() -> callRecordingService.createPresignedUrl(call.getId(), 99L, request))
                    .isInstanceOf(CallParticipantMismatchException.class);
        }

        @Test
        @DisplayName("통화가 아직 진행 중이면 CallActiveException")
        void createPresignedUrl_whenCallActive_throws() {
            // given
            final Call active = callRepository.save(Call.start(1L, 2L, UUID.randomUUID(), STARTED_AT));
            final CallRecordingPresignedUrlRequest request =
                    new CallRecordingPresignedUrlRequest("audio/m4a", 1024L);

            // when & then
            assertThatThrownBy(() -> callRecordingService.createPresignedUrl(active.getId(), 1L, request))
                    .isInstanceOf(CallActiveException.class);
        }

        @Test
        @DisplayName("허용 목록에 없는 contentType이면 INVALID_CALL_RECORDING_CONTENT_TYPE 400")
        void createPresignedUrl_whenContentTypeDisallowed_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            final CallRecordingPresignedUrlRequest request =
                    new CallRecordingPresignedUrlRequest("audio/wav", 1024L);

            // when & then
            assertThatThrownBy(() -> callRecordingService.createPresignedUrl(call.getId(), 1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_CALL_RECORDING_CONTENT_TYPE);
        }

        @Test
        @DisplayName("최대 크기 초과면 CALL_RECORDING_TOO_LARGE 400")
        void createPresignedUrl_whenTooLarge_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            final long oversize = 52_428_800L + 1;
            final CallRecordingPresignedUrlRequest request =
                    new CallRecordingPresignedUrlRequest("audio/m4a", oversize);

            // when & then
            assertThatThrownBy(() -> callRecordingService.createPresignedUrl(call.getId(), 1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CALL_RECORDING_TOO_LARGE);
        }
    }

    @Nested
    @DisplayName("create: 업로드 완료 후 메타데이터 저장")
    class Create {

        @Test
        @DisplayName("정상 요청 시 응답을 반환하고 DB에 row를 저장한다")
        void create_whenValid_createsRecording() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            final String key = "call-recordings/%d/1/upload-uuid".formatted(call.getId());
            storage.put(key, "audio/m4a");

            // when
            final CallRecordingCreateResponse response = callRecordingService.create(
                    call.getId(), 1L, new CallRecordingCreateRequest(key));

            // then
            assertThat(response.status()).isEqualTo(CallRecordingStatus.UPLOADED);
            final CallRecording saved = callRecordingRepository.findByCallIdAndUserId(call.getId(), 1L).orElseThrow();
            assertThat(saved.getRecordingKey()).isEqualTo(key);
            assertThat(saved.getContentType()).isEqualTo("audio/m4a");
            assertThat(saved.getStatus()).isEqualTo(CallRecordingStatus.UPLOADED);
        }

        @Test
        @DisplayName("동일 (callId, userId) 재요청은 기존 row를 그대로 반환하고 새 row를 만들지 않는다 (멱등)")
        void create_whenAlreadyExists_returnsExisting() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            final String key = "call-recordings/%d/1/first-upload".formatted(call.getId());
            storage.put(key, "audio/m4a");
            final CallRecordingCreateResponse first = callRecordingService.create(
                    call.getId(), 1L, new CallRecordingCreateRequest(key));

            // when
            final CallRecordingCreateResponse second = callRecordingService.create(
                    call.getId(), 1L, new CallRecordingCreateRequest(key));

            // then
            assertThat(second.recordingId()).isEqualTo(first.recordingId());
            assertThat(callRecordingRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("존재하지 않는 callId면 CallNotFoundException")
        void create_whenCallMissing_throws() {
            // when & then
            assertThatThrownBy(() -> callRecordingService.create(
                    9999L, 1L, new CallRecordingCreateRequest("call-recordings/9999/1/k")))
                    .isInstanceOf(CallNotFoundException.class);
        }

        @Test
        @DisplayName("참여자가 아니면 CallParticipantMismatchException")
        void create_whenNotParticipant_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            final String key = "call-recordings/%d/99/k".formatted(call.getId());

            // when & then
            assertThatThrownBy(() -> callRecordingService.create(
                    call.getId(), 99L, new CallRecordingCreateRequest(key)))
                    .isInstanceOf(CallParticipantMismatchException.class);
        }

        @Test
        @DisplayName("다른 사용자의 key를 사용하면 CallRecordingKeyForbiddenException")
        void create_whenKeyNotOwned_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            final String foreignKey = "call-recordings/%d/2/their-upload".formatted(call.getId());

            // when & then: userId=1이 userId=2의 key를 사용
            assertThatThrownBy(() -> callRecordingService.create(
                    call.getId(), 1L, new CallRecordingCreateRequest(foreignKey)))
                    .isInstanceOf(CallRecordingKeyForbiddenException.class);
        }

        @Test
        @DisplayName("S3에 파일이 없으면 CallRecordingS3MissingException")
        void create_whenS3Missing_throws() {
            // given
            final Call call = saveEndedCall(1L, 2L);
            final String key = "call-recordings/%d/1/never-uploaded".formatted(call.getId());
            // storage.put 호출 안 함 → S3 미존재 상태

            // when & then
            assertThatThrownBy(() -> callRecordingService.create(
                    call.getId(), 1L, new CallRecordingCreateRequest(key)))
                    .isInstanceOf(CallRecordingS3MissingException.class);
        }
    }

    private Call saveEndedCall(final Long userA, final Long userB) {
        final Call call = callRepository.save(Call.start(userA, userB, UUID.randomUUID(), STARTED_AT));
        call.end(ENDED_AT);
        return callRepository.save(call);
    }
}
