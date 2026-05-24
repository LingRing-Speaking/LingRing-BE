package com.lingring.domain.callanalysis.api;

import com.lingring.domain.callanalysis.dto.response.CallAnalysisStatusResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "CallAnalysisStatus", description = "통화 LLM 분석 상태 조회 API")
public interface CallAnalysisStatusApi {

    @Operation(
            summary = "분석 상태 조회",
            description = "본인 소유 분석의 진행 상태(PROCESSING/COMPLETED/FAILED)만 가볍게 조회한다."
                    + " 폴링용. 본인 소유가 아닌 analysisId 요청은 403."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/analyses/{analysisId}/status")
    ApiResponse<CallAnalysisStatusResponse> getStatus(
            @AuthUser final Long userId,
            @Parameter(description = "분석 ID", example = "100")
            @PathVariable final Long analysisId
    );
}
