package com.lingring.domain.userreport.api;

import com.lingring.domain.userreport.dto.request.UserReportCreateRequest;
import com.lingring.domain.userreport.dto.response.UserReportResponse;
import com.lingring.domain.userreport.service.UserReportService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserReportController implements UserReportApi {

    private final UserReportService userReportService;

    @Override
    public ApiResponse<UserReportResponse> report(
            final Long userId,
            final UserReportCreateRequest request
    ) {
        return ApiResponse.success(HttpStatus.CREATED, userReportService.report(userId, request));
    }
}
