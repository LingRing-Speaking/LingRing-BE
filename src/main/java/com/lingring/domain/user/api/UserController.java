package com.lingring.domain.user.api;

import com.lingring.domain.user.dto.request.PresignedUrlRequest;
import com.lingring.domain.user.dto.request.UpdateProfileRequest;
import com.lingring.domain.user.dto.request.WithdrawRequest;
import com.lingring.domain.user.dto.response.MeResponse;
import com.lingring.domain.user.dto.response.PresignedUrlResponse;
import com.lingring.domain.user.dto.response.UpdateProfileResponse;
import com.lingring.domain.user.dto.response.UserProfileResponse;
import com.lingring.domain.user.service.UserService;
import com.lingring.domain.user.service.UserWithdrawalService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;
    private final UserWithdrawalService userWithdrawalService;

    @Override
    public ApiResponse<MeResponse> getMe(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, userService.getMe(userId));
    }

    @Override
    public ApiResponse<UserProfileResponse> getUserProfile(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, userService.getUserProfile(userId));
    }

    @Override
    public ApiResponse<Void> withdraw(final Long userId, final WithdrawRequest request) {
        userWithdrawalService.withdraw(userId, request.reason(), request.description());
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }

    @Override
    public ApiResponse<PresignedUrlResponse> createProfileImageUploadUrl(
            final Long userId,
            final PresignedUrlRequest request
    ) {
        return ApiResponse.success(HttpStatus.OK, userService.createProfileImageUploadUrl(userId, request));
    }

    @Override
    public ApiResponse<UpdateProfileResponse> updateProfile(
            final Long userId,
            final UpdateProfileRequest request
    ) {
        return ApiResponse.success(HttpStatus.OK, userService.updateProfile(userId, request));
    }
}
