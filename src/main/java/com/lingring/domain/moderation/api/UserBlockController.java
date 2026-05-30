package com.lingring.domain.moderation.api;

import com.lingring.domain.moderation.dto.request.UserBlockCreateRequest;
import com.lingring.domain.moderation.dto.response.UserBlocksResponse;
import com.lingring.domain.moderation.dto.response.UserBlockResponse;
import com.lingring.domain.moderation.service.UserBlockService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserBlockController implements UserBlockApi {

    private final UserBlockService userBlockService;

    @Override
    public ApiResponse<UserBlockResponse> block(
            final Long userId,
            final UserBlockCreateRequest request
    ) {
        return ApiResponse.success(HttpStatus.CREATED, userBlockService.block(userId, request));
    }

    @Override
    public ApiResponse<Void> unblock(final Long userId, final Long blockedUserId) {
        userBlockService.unblock(userId, blockedUserId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }

    @Override
    public ApiResponse<UserBlocksResponse> getAll(
            final Long userId,
            final int page,
            final int size
    ) {
        return ApiResponse.success(HttpStatus.OK, userBlockService.getAllByUserId(userId, page, size));
    }
}
