package com.lingring.domain.expression.api;

import com.lingring.domain.expression.dto.response.RecommendedExpressionResponse;
import com.lingring.domain.expression.service.RecommendedExpressionService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RecommendedExpressionController implements RecommendedExpressionApi {

    private final RecommendedExpressionService recommendedExpressionService;

    @Override
    public ApiResponse<RecommendedExpressionResponse> getDaily(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, recommendedExpressionService.getDaily(userId));
    }
}
