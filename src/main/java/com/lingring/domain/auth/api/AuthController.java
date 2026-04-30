package com.lingring.domain.auth.api;

import com.lingring.domain.auth.dto.request.SocialLoginRequest;
import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.service.AuthService;
import com.lingring.global.common.response.ApiResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @Override
    public ApiResponse<AuthTokenResponse> socialLogin(final SocialLoginRequest request) {
        return ApiResponse.success(HttpStatus.OK, authService.socialLogin(request));
    }

    @Override
    public ApiResponse<AuthTokenResponse> refresh(final String authorizationHeader) {
        final String refreshToken = stripBearer(authorizationHeader);
        return ApiResponse.success(HttpStatus.OK, authService.refresh(refreshToken));
    }

    @Override
    public ApiResponse<Void> logout(final Long userId) {
        authService.logout(userId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }

    private static String stripBearer(final String header) {
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException(
                    ErrorCode.INVALID_TOKEN,
                    "Authorization 헤더가 없거나 Bearer 형식이 아닙니다."
            );
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
