package com.lingring.domain.auth.api;

import com.lingring.domain.auth.dto.request.RefreshRequest;
import com.lingring.domain.auth.dto.request.SocialLoginRequest;
import com.lingring.domain.auth.dto.response.AuthTokenResponse;
import com.lingring.domain.auth.dto.response.TokenPairResponse;
import com.lingring.domain.auth.service.AuthService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ApiResponse<AuthTokenResponse> socialLogin(final SocialLoginRequest request) {
        return ApiResponse.success(HttpStatus.OK, authService.socialLogin(request));
    }

    @Override
    public ApiResponse<TokenPairResponse> refresh(final RefreshRequest request) {
        return ApiResponse.success(HttpStatus.OK, authService.refresh(request.refreshToken()));
    }

    @Override
    public ApiResponse<Void> logout(final Long userId) {
        authService.logout(userId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }
}
