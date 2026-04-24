package com.lingring.domain.savedexpression.api;

import com.lingring.domain.savedexpression.dto.request.SavedExpressionCreateRequest;
import com.lingring.domain.savedexpression.dto.response.SavedExpressionListResponse;
import com.lingring.domain.savedexpression.dto.response.SavedExpressionResponse;
import com.lingring.domain.savedexpression.service.SavedExpressionService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SavedExpressionController implements SavedExpressionApi {

    private final SavedExpressionService savedExpressionService;

    @Override
    public ApiResponse<SavedExpressionResponse> create(
            final Long userId,
            final SavedExpressionCreateRequest request
    ) {
        return ApiResponse.success(HttpStatus.CREATED, savedExpressionService.save(userId, request));
    }

    @Override
    public ApiResponse<SavedExpressionListResponse> getAll(
            final Long userId,
            final int page,
            final int size
    ) {
        return ApiResponse.success(HttpStatus.OK, savedExpressionService.getAllByUserId(userId, page, size));
    }

    @Override
    public ApiResponse<Void> delete(final Long userId, final Long id) {
        savedExpressionService.delete(userId, id);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }
}
