package com.lingring.domain.call.api;

import com.lingring.domain.call.dto.response.CallTranscriptResponse;
import com.lingring.domain.call.dto.response.CallTranscriptStartResponse;
import com.lingring.domain.call.service.CallTranscriptService;
import com.lingring.domain.callanalysis.facade.CallAnalysisRequestFacade;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CallTranscriptController implements CallTranscriptApi {

    private final CallAnalysisRequestFacade callAnalysisRequestFacade;
    private final CallTranscriptService callTranscriptService;

    @Override
    public ApiResponse<CallTranscriptStartResponse> requestAnalysis(
            final Long userId,
            final Long callId
    ) {
        return ApiResponse.success(
                HttpStatus.ACCEPTED,
                callAnalysisRequestFacade.request(callId, userId)
        );
    }

    @Override
    public ApiResponse<CallTranscriptResponse> getTranscript(
            final Long userId,
            final Long callId
    ) {
        return ApiResponse.success(
                HttpStatus.OK,
                callTranscriptService.getTranscript(callId, userId)
        );
    }
}
