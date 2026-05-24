package com.lingring.domain.callanalysis.api;

import com.lingring.domain.callanalysis.dto.response.CallAnalysisStatusResponse;
import com.lingring.domain.callanalysis.service.CallAnalysisService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CallAnalysisStatusController implements CallAnalysisStatusApi {

    private final CallAnalysisService callAnalysisService;

    @Override
    public ApiResponse<CallAnalysisStatusResponse> getStatus(final Long userId, final Long analysisId) {
        return ApiResponse.success(
                HttpStatus.OK,
                callAnalysisService.getStatus(analysisId, userId)
        );
    }
}
