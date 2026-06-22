package com.lingring.domain.review.api;

import com.lingring.domain.review.dto.response.CallTranscriptResponse;
import com.lingring.domain.review.service.CallTranscriptService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CallTranscriptController implements CallTranscriptApi {

    private final CallTranscriptService callTranscriptService;

    @Override
    public ApiResponse<CallTranscriptResponse> getTranscript(final Long userId, final Long callId) {
        return ApiResponse.success(
                HttpStatus.OK,
                callTranscriptService.getTranscript(callId, userId)
        );
    }
}
