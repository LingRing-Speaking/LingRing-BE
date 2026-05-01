package com.lingring.domain.user.api;

import com.lingring.domain.user.dto.response.UserStatsResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "UserStats", description = "사용자 통계 API")
public interface UserStatsApi {

    @Operation(
            summary = "내 통계 조회",
            description = "인증된 사용자의 통계 정보(레벨, 매너 온도, 통화 수, 연속 학습일, 저장한 표현 수, 마지막 학습일)를 조회한다."
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
    @GetMapping("/me/stats")
    ApiResponse<UserStatsResponse> getStats(
            @AuthUser final Long userId
    );
}
