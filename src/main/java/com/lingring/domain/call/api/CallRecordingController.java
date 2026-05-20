package com.lingring.domain.call.api;

import com.lingring.domain.call.dto.request.CallRecordingCreateRequest;
import com.lingring.domain.call.dto.request.CallRecordingPresignedUrlRequest;
import com.lingring.domain.call.dto.response.CallRecordingCreateResponse;
import com.lingring.domain.call.dto.response.CallRecordingPresignedUrlResponse;
import com.lingring.domain.call.service.CallRecordingCreateResult;
import com.lingring.domain.call.service.CallRecordingService;
import com.lingring.global.common.response.ApiResponse;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CallRecordingController implements CallRecordingApi {

    private final CallRecordingService callRecordingService;

    @Override
    public ApiResponse<CallRecordingPresignedUrlResponse> createPresignedUrl(
            final Long userId,
            final Long callId,
            final CallRecordingPresignedUrlRequest request
    ) {
        return ApiResponse.success(HttpStatus.OK,
                callRecordingService.createPresignedUrl(callId, userId, request));
    }

    @Override
    public ResponseEntity<ApiResponse<CallRecordingCreateResponse>> create(
            final Long userId,
            final Long callId,
            final CallRecordingCreateRequest request
    ) {
        final CallRecordingCreateResult result = callRecordingService.create(callId, userId, request);
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .location(URI.create("/calls/%d/recordings/%d".formatted(
                            callId, result.response().recordingId())))
                    .body(ApiResponse.success(HttpStatus.CREATED, result.response()));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, result.response()));
    }
}
