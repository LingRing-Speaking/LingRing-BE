package com.lingring.domain.recommendedexpression.api;

import com.lingring.domain.recommendedexpression.dto.response.RecommendedExpressionResponse;
import com.lingring.domain.recommendedexpression.service.RecommendedExpressionService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecommendedExpressionController implements RecommendedExpressionApi {

    private final RecommendedExpressionService recommendedExpressionService;

    @Override
    public ApiResponse<RecommendedExpressionResponse> getDaily() {
        return ApiResponse.success(HttpStatus.OK, recommendedExpressionService.getDaily());
    }
}
