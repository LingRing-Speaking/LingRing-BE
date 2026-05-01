package com.lingring.domain.userreport.api;

import com.lingring.domain.userreport.dto.request.UserReportCreateRequest;
import com.lingring.domain.userreport.dto.response.UserReportResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "UserReport", description = "사용자 신고 API")
public interface UserReportApi {

    @Operation(
            summary = "사용자 신고",
            description = "인증된 사용자가 reportedUserId의 사용자를 신고한다. 신고 시 동일 대상에 대한 차단도 자동으로 등록된다 (이미 차단되어 있으면 차단은 변경되지 않음)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "신고 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/BadRequest"
            )
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/reports")
    ApiResponse<UserReportResponse> report(
            @AuthUser final Long userId,
            @Valid @RequestBody final UserReportCreateRequest request
    );
}
