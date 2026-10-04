package com.lingring.domain.user.api;

import com.lingring.domain.user.dto.request.NotificationSettingUpdateRequest;
import com.lingring.domain.user.dto.response.NotificationSettingResponse;
import com.lingring.domain.user.service.NotificationSettingService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationSettingController implements NotificationSettingApi {

    private final NotificationSettingService notificationSettingService;

    @Override
    public ApiResponse<NotificationSettingResponse> update(
            final Long userId,
            final NotificationSettingUpdateRequest request
    ) {
        return ApiResponse.success(HttpStatus.OK, notificationSettingService.update(userId, request));
    }
}
