package com.lingring.domain.presence.api;

import com.lingring.domain.presence.dto.response.HeartbeatResponse;
import com.lingring.domain.presence.facade.PresenceFacade;
import com.lingring.domain.presence.service.PresenceService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PresenceController implements PresenceApi {

    private final PresenceFacade presenceFacade;
    private final PresenceService presenceService;

    @Override
    public ApiResponse<HeartbeatResponse> heartbeat(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, presenceFacade.heartbeat(userId));
    }

    @Override
    public ApiResponse<Void> disconnect(final Long userId) {
        presenceService.disconnect(userId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }
}
