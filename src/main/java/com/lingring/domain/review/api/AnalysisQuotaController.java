package com.lingring.domain.review.api;

import com.lingring.domain.review.dto.response.AnalysisQuotaResponse;
import com.lingring.domain.review.service.AnalysisQuotaService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnalysisQuotaController implements AnalysisQuotaApi {

    private final AnalysisQuotaService analysisQuotaService;

    @Override
    public ApiResponse<AnalysisQuotaResponse> getMyQuota(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, analysisQuotaService.getStatus(userId));
    }
}
