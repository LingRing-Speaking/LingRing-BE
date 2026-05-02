package com.lingring.domain.call.api;

import com.lingring.domain.call.dto.response.CallsResponse;
import com.lingring.domain.call.service.CallService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CallController implements CallApi {

    private final CallService callService;

    @Override
    public ApiResponse<CallsResponse> getAll(
            final Long userId,
            final int page,
            final int size
    ) {
        return ApiResponse.success(HttpStatus.OK, callService.getCallsByUserId(userId, page, size));
    }
}
