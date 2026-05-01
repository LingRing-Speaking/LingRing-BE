package com.lingring.domain.user.api;

import com.lingring.domain.user.dto.response.MeResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "User", description = "사용자 API")
public interface UserApi {

    @Operation(
            summary = "내 정보 조회",
            description = "Access token 유효성 검증과 함께 소유자 정보(id, nickname)를 반환한다. FE 부팅 시 자동 로그인 흐름에서 사용."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me")
    ApiResponse<MeResponse> getMe(
            @AuthUser final Long userId
    );
}
