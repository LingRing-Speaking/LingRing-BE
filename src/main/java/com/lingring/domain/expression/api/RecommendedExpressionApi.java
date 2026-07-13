package com.lingring.domain.expression.api;

import com.lingring.domain.expression.dto.response.RecommendedExpressionResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "RecommendedExpression", description = "추천 표현 API")
public interface RecommendedExpressionApi {

    @Operation(
            summary = "오늘의 추천 표현 조회",
            description = "Asia/Seoul 기준 날짜를 시드로 매일 1개 추천 표현을 반환한다. "
                    + "같은 날 호출하면 같은 결과가 반환된다 (모든 사용자 공통). "
                    + "bookmarkId는 호출자가 찜(저장)한 경우 해당 저장 표현 id, 아니면 null이다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    ref = "#/components/responses/NotFound"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/recommended-expressions/daily")
    ApiResponse<RecommendedExpressionResponse> getDaily(@AuthUser final Long userId);
}
