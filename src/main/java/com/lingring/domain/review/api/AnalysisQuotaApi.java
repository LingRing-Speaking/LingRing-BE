package com.lingring.domain.review.api;

import com.lingring.domain.review.dto.response.AnalysisQuotaResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "AnalysisQuota", description = "분석 쿼터 API")
public interface AnalysisQuotaApi {

    @Operation(
            summary = "분석 잔여 횟수 조회",
            description = "로그인 사용자의 오늘 남은 무료 티켓과 유료 티켓,"
                    + " 그리고 무료가 리필되는 다음 0시(Asia/Seoul)를 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me/analysis-quota")
    ApiResponse<AnalysisQuotaResponse> getMyQuota(@AuthUser final Long userId);
}
