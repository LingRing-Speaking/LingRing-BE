package com.lingring.domain.callanalysis.api;

import com.lingring.domain.callanalysis.dto.request.CallRecordingCreateRequest;
import com.lingring.domain.callanalysis.dto.request.CallRecordingPresignedUrlRequest;
import com.lingring.domain.callanalysis.dto.response.CallRecordingCreateResponse;
import com.lingring.domain.callanalysis.dto.response.CallRecordingPresignedUrlResponse;
import com.lingring.domain.callanalysis.service.CallRecordingService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ApiResponse<CallRecordingCreateResponse> create(
            final Long userId,
            final Long callId,
            final CallRecordingCreateRequest request
    ) {
        return ApiResponse.success(HttpStatus.CREATED,
                callRecordingService.create(callId, userId, request));
    }
}
