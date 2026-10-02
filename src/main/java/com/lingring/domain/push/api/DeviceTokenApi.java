package com.lingring.domain.push.api;

import com.lingring.domain.push.dto.request.DeviceTokenRegisterRequest;
import com.lingring.domain.push.dto.request.DeviceTokenUnregisterRequest;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "DeviceToken", description = "푸시 디바이스 토큰 API")
public interface DeviceTokenApi {

    @Operation(
            summary = "디바이스 토큰 등록",
            description = """
                    FCM 등록 토큰을 token 기준으로 upsert한다. 같은 토큰이 다른 유저 소유면 요청한 유저로 소유자를 옮긴다.
                    한 유저가 여러 기기의 토큰을 가질 수 있고, 같은 요청을 반복해도 결과가 같다.
                    platform은 ios / android (대소문자 무관). 마케팅 푸시 동의 여부와 관계없이 받는다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "등록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "token 누락·길이 초과 또는 지원하지 않는 platform"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/me/device-tokens")
    ApiResponse<Void> register(
            @AuthUser final Long userId,
            @Valid @RequestBody final DeviceTokenRegisterRequest request
    );

    @Operation(
            summary = "디바이스 토큰 해제",
            description = """
                    로그아웃 시 /auth/logout보다 먼저 호출한다. 내 소유 토큰일 때만 삭제하며,
                    토큰이 없거나 다른 유저 소유여도 204를 반환한다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "해제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "token 누락"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/me/device-tokens/unregister")
    ApiResponse<Void> unregister(
            @AuthUser final Long userId,
            @Valid @RequestBody final DeviceTokenUnregisterRequest request
    );
}
