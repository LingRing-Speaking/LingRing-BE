package com.lingring.domain.user.api;

import com.lingring.domain.user.dto.response.UserStatsResponse;
import com.lingring.domain.user.service.UserStatsService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserStatsController implements UserStatsApi {

    private final UserStatsService userStatsService;

    @Override
    public ApiResponse<UserStatsResponse> getStats(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, userStatsService.getByUserId(userId));
    }
}
