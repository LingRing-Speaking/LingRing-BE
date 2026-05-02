package com.lingring.domain.user.api;

import com.lingring.domain.user.dto.response.MeResponse;
import com.lingring.domain.user.dto.response.UserProfileResponse;
import com.lingring.domain.user.service.UserService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ApiResponse<MeResponse> getMe(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, userService.getMe(userId));
    }

    @Override
    public ApiResponse<UserProfileResponse> getUserProfile(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, userService.getUserProfile(userId));
    }
}
