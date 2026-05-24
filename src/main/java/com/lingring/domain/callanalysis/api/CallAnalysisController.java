package com.lingring.domain.callanalysis.api;

import com.lingring.domain.call.dto.response.CallTranscriptStartResponse;
import com.lingring.domain.callanalysis.dto.response.CallAnalysisResponse;
import com.lingring.domain.callanalysis.facade.CallAnalysisRequestFacade;
import com.lingring.domain.callanalysis.service.CallAnalysisService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CallAnalysisController implements CallAnalysisApi {

    private final CallAnalysisRequestFacade callAnalysisRequestFacade;
    private final CallAnalysisService callAnalysisService;

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
    public ApiResponse<CallAnalysisResponse> getAnalysis(final Long userId, final Long callId) {
        return ApiResponse.success(
                HttpStatus.OK,
                callAnalysisService.get(callId, userId)
        );
    }
}
