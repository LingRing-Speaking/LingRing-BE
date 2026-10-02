package com.lingring.domain.push.api;

import com.lingring.domain.push.dto.request.DeviceTokenRegisterRequest;
import com.lingring.domain.push.dto.request.DeviceTokenUnregisterRequest;
import com.lingring.domain.push.service.DeviceTokenService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeviceTokenController implements DeviceTokenApi {

    private final DeviceTokenService deviceTokenService;

    @Override
    public ApiResponse<Void> register(final Long userId, final DeviceTokenRegisterRequest request) {
        deviceTokenService.register(userId, request);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }

    @Override
    public ApiResponse<Void> unregister(final Long userId, final DeviceTokenUnregisterRequest request) {
        deviceTokenService.unregister(userId, request);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }
}
