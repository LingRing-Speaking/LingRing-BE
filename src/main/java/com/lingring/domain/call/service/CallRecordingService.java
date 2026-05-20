package com.lingring.domain.call.service;

import com.lingring.domain.call.dao.CallRecordingRepository;
import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.call.domain.CallRecording;
import com.lingring.domain.call.domain.CallRecordingStorage;
import com.lingring.domain.call.domain.PresignedUpload;
import com.lingring.domain.call.domain.vo.CallRecordingKey;
import com.lingring.domain.call.dto.request.CallRecordingCreateRequest;
import com.lingring.domain.call.dto.request.CallRecordingPresignedUrlRequest;
import com.lingring.domain.call.dto.response.CallRecordingCreateResponse;
import com.lingring.domain.call.dto.response.CallRecordingPresignedUrlResponse;
import com.lingring.domain.call.exception.CallActiveException;
import com.lingring.domain.call.exception.CallNotFoundException;
import com.lingring.domain.call.exception.CallParticipantMismatchException;
import com.lingring.domain.call.exception.CallRecordingS3MissingException;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.infrastructure.s3.CallRecordingS3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CallRecordingService {

    private final CallRepository callRepository;
    private final CallRecordingRepository callRecordingRepository;
    private final CallRecordingStorage storage;
    private final CallRecordingS3Properties properties;

    @Transactional(readOnly = true)
    public CallRecordingPresignedUrlResponse createPresignedUrl(
            final Long callId,
            final Long userId,
            final CallRecordingPresignedUrlRequest request
    ) {
        final Call call = requireParticipantCall(callId, userId);
        if (call.isActive()) {
            throw new CallActiveException(callId);
        }
        requireAllowedContentType(request.contentType());
        requireWithinSizeLimit(request.contentLength());

        final CallRecordingKey key = CallRecordingKey.generateFor(callId, userId);
        final PresignedUpload presigned = storage.generateUploadUrl(key.getValue(), request.contentType(), request.contentLength());
        return CallRecordingPresignedUrlResponse.from(presigned);
    }

    @Transactional
    public CallRecordingCreateResponse create(
            final Long callId,
            final Long userId,
            final CallRecordingCreateRequest request
    ) {
        requireParticipantCall(callId, userId);
        CallRecordingKey.from(request.recordingKey()).requireOwnedBy(callId, userId);
        if (!storage.exists(request.recordingKey())) {
            throw new CallRecordingS3MissingException(request.recordingKey());
        }

        final CallRecording recording = callRecordingRepository
                .findByCallIdAndUserId(callId, userId)
                .orElseGet(() -> callRecordingRepository.save(
                        CallRecording.upload(callId, userId, request.recordingKey(),
                                storage.contentTypeOf(request.recordingKey()))));
        return CallRecordingCreateResponse.from(recording);
    }

    private Call requireParticipantCall(final Long callId, final Long userId) {
        final Call call = callRepository.findById(callId)
                .orElseThrow(() -> new CallNotFoundException(callId));
        if (!call.involves(userId)) {
            throw new CallParticipantMismatchException(callId, userId);
        }
        return call;
    }

    private void requireAllowedContentType(final String contentType) {
        if (!properties.allowedContentTypes().contains(contentType)) {
            throw new BadRequestException(
                    ErrorCode.INVALID_CALL_RECORDING_CONTENT_TYPE,
                    "허용되지 않은 오디오 형식입니다: %s".formatted(contentType)
            );
        }
    }

    private void requireWithinSizeLimit(final long contentLength) {
        if (contentLength > properties.maxContentLength()) {
            throw new BadRequestException(
                    ErrorCode.CALL_RECORDING_TOO_LARGE,
                    "녹음 파일 크기가 허용 범위(%d bytes)를 초과했습니다: %d".formatted(
                            properties.maxContentLength(), contentLength)
            );
        }
    }
}
