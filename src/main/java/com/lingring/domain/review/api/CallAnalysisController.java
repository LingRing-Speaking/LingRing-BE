package com.lingring.domain.review.api;

import com.lingring.domain.review.dto.response.CallAnalysisResponse;
import com.lingring.domain.review.dto.response.CallAnalysisStartResponse;
import com.lingring.domain.review.dto.response.CallAnalysisStatusResponse;
import com.lingring.domain.review.facade.CallAnalysisRequestFacade;
import com.lingring.domain.review.service.CallAnalysisService;
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
    public ApiResponse<CallAnalysisStartResponse> requestAnalysis(
            final Long userId,
            final Long callId
    ) {
        return ApiResponse.success(
                HttpStatus.ACCEPTED,
                callAnalysisRequestFacade.request(callId, userId)
        );
    }

    @Override
    public ApiResponse<CallAnalysisResponse> getAnalysis(final Long userId, final Long analysisId) {
        return ApiResponse.success(
                HttpStatus.OK,
                callAnalysisService.get(analysisId, userId)
        );
    }

    @Override
    public ApiResponse<CallAnalysisStatusResponse> getStatus(final Long userId, final Long analysisId) {
        return ApiResponse.success(
                HttpStatus.OK,
                callAnalysisService.getStatus(analysisId, userId)
        );
    }
}
