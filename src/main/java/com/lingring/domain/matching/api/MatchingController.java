package com.lingring.domain.matching.api;

import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.domain.matching.service.MatchingService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MatchingController implements MatchingApi {

    private final MatchingService matchingService;

    @Override
    public ApiResponse<Void> enterQueue(final Long userId) {
        matchingService.enterQueue(userId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }

    @Override
    public ApiResponse<MatchingStatusResponse> getStatus(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, matchingService.getStatus(userId));
    }

    @Override
    public ApiResponse<Void> leaveQueue(final Long userId) {
        matchingService.leaveQueue(userId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }
}
