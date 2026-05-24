package com.lingring.domain.callanalysis.api;

import com.lingring.domain.callanalysis.dto.response.CallAnalysisResponse;
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

@Tag(name = "CallAnalysis", description = "통화 LLM 분석 결과 API")
public interface CallAnalysisApi {

    @Operation(
            summary = "통화 분석 결과 조회",
            description = "STT 완료 후 비동기로 생성된 LLM 분석 결과를 화자별로 조회한다."
                    + " status에 따라 PROCESSING(아직 생성 중) / COMPLETED(mistakes/positives 포함) / FAILED 응답이 반환된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/calls/{callId}/analysis")
    ApiResponse<CallAnalysisResponse> getAnalysis(
            @AuthUser final Long userId,
            @Parameter(description = "통화 ID", example = "42")
            @PathVariable final Long callId
    );
}
