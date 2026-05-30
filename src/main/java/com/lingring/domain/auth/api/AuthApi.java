package com.lingring.domain.auth.api;

import com.lingring.domain.auth.dto.request.DemoLoginRequest;
import com.lingring.domain.auth.dto.request.RefreshRequest;
import com.lingring.domain.auth.dto.request.SocialLoginRequest;
import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.dto.response.TokenPairResponse;
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
            summary = "Apple App Review 리뷰어용 demo 로그인",
            description = """
                    SIWA·Kakao 전용 앱이라 진짜 Apple ID/Kakao 계정 발급은 2FA 때문에
                    비현실적이라 BE Test Account Bypass 패턴을 사용한다.
                    환경변수에 매핑된 토큰을 받으면 미리 시드된 demo user 의 자체 JWT 를 발급한다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "demo 로그인 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/BadRequest"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 데모 토큰"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "데모 로그인 비활성화"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/auth/demo-login")
    ApiResponse<AuthTokenResponse> demoLogin(
            @Valid @RequestBody final DemoLoginRequest request
    );

    @Operation(
            summary = "토큰 갱신",
            description = "요청 body의 refresh token으로 신규 access/refresh 토큰 쌍을 발급한다 (1회용 회전, stale 재사용 시 전체 세션 무효화)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "갱신 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/BadRequest"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/auth/refresh")
    ApiResponse<TokenPairResponse> refresh(
            @Valid @RequestBody final RefreshRequest request
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
