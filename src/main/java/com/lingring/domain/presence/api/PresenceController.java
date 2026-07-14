package com.lingring.domain.presence.api;

import com.lingring.domain.presence.service.PresenceService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PresenceController implements PresenceApi {

    private final PresenceService presenceService;

    @Override
    public ApiResponse<Void> heartbeat(final Long userId) {
        presenceService.heartbeat(userId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }

    @Override
    public ApiResponse<Void> disconnect(final Long userId) {
        presenceService.disconnect(userId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }
}
