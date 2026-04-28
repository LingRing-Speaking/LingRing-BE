package com.lingring.domain.icebreaker.api;

import com.lingring.domain.icebreaker.dto.response.IcebreakerListResponse;
import com.lingring.domain.icebreaker.service.IcebreakerService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IcebreakerController implements IcebreakerApi {

    private final IcebreakerService icebreakerService;

    @Override
    public ApiResponse<IcebreakerListResponse> getRandom(final int count) {
        return ApiResponse.success(HttpStatus.OK, icebreakerService.getRandom(count));
    }
}
