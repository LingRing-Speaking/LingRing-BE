package com.lingring.domain.auth.api;

import com.lingring.domain.auth.dto.request.SocialLoginRequest;
import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.auth.annotation.RefreshToken;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Auth", description = "소셜 로그인·토큰 갱신·로그아웃 API")
public interface AuthApi {

    @Operation(
            summary = "소셜 로그인 (가입 or 로그인)",
            description = "IdP의 id_token을 검증하고 LingRing 자체 JWT를 발급한다. 신규 사용자는 같이 보낸 nickname으로 가입된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인/가입 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/BadRequest"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "닉네임 중복 — FE는 새 닉네임으로 재시도"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "IdP 인증 서버 통신 실패"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/auth/social")
    ApiResponse<AuthTokenResponse> socialLogin(
            @Valid @RequestBody final SocialLoginRequest request
    );

    @Operation(
            summary = "토큰 갱신",
            description = "Authorization 헤더의 refresh token으로 신규 access/refresh 토큰 쌍을 발급한다 (회전)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "갱신 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/auth/refresh")
    ApiResponse<AuthTokenResponse> refresh(
            @Parameter(
                    in = ParameterIn.HEADER,
                    name = HttpHeaders.AUTHORIZATION,
                    required = true,
                    description = "Bearer <refreshToken>"
            )
            @RefreshToken final String refreshToken
    );

    @Operation(
            summary = "로그아웃",
            description = "서버 측 refresh token을 무효화한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "로그아웃 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/auth/logout")
    ApiResponse<Void> logout(
            @AuthUser final Long userId
    );
}
