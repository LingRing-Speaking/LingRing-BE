package com.lingring.domain.push.api;

import com.lingring.domain.push.service.PushSendService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PushTestController implements PushTestApi {

    private final PushSendService pushSendService;

    @Override
    public ApiResponse<Void> sendTest(final Long userId) {
        pushSendService.sendTest(userId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }
}
