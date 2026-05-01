package com.lingring.domain.userexpression.api;

import com.lingring.domain.userexpression.dto.request.UserExpressionCreateRequest;
import com.lingring.domain.userexpression.dto.response.UserExpressionListResponse;
import com.lingring.domain.userexpression.dto.response.UserExpressionResponse;
import com.lingring.domain.userexpression.service.UserExpressionService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserExpressionController implements UserExpressionApi {

    private final UserExpressionService userExpressionService;

    @Override
    public ApiResponse<UserExpressionResponse> create(
            final Long userId,
            final UserExpressionCreateRequest request
    ) {
        return ApiResponse.success(HttpStatus.CREATED, userExpressionService.save(userId, request));
    }

    @Override
    public ApiResponse<UserExpressionListResponse> getAll(
            final Long userId,
            final int page,
            final int size
    ) {
        return ApiResponse.success(HttpStatus.OK, userExpressionService.getAllByUserId(userId, page, size));
    }

    @Override
    public ApiResponse<Void> delete(final Long userId, final Long expressionId) {
        userExpressionService.delete(userId, expressionId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }
}
