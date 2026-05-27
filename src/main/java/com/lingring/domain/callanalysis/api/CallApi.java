package com.lingring.domain.callanalysis.api;

import com.lingring.domain.callanalysis.dto.response.CallsResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Call", description = "통화 API")
public interface CallApi {

    @Operation(
            summary = "내 통화 목록 조회",
            description = "인증된 사용자가 참여한 종료된 통화를 최근순(startedAt DESC)으로 반환한다. 무한 스크롤용 Slice. size는 서버에서 최대 50으로 clamp된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/calls")
    ApiResponse<CallsResponse> getAll(
            @AuthUser final Long userId,
            @Parameter(description = "0-based 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") final int page,
            @Parameter(description = "페이지 크기 (1 ~ 50, 기본 20)", example = "20")
            @RequestParam(defaultValue = "20") final int size
    );
}
