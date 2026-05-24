package com.lingring.domain.callanalysis.api;

import com.lingring.domain.call.dto.response.CallTranscriptStartResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "CallAnalysis", description = "통화 LLM 분석 API")
public interface CallAnalysisApi {

    @Operation(
            summary = "분석 트리거",
            description = "두 화자의 녹음이 모두 업로드된 통화에 대해 STT + LLM 분석 파이프라인을 비동기로 시작한다."
                    + " 이미 처리 중이거나 완료된 경우 기존 정보를 그대로 반환한다 (멱등)."
                    + " 응답 body의 status 필드로 PROCESSING/COMPLETED 여부를 확인할 수 있다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "분석 시작 또는 기존 진행 상태 반환",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/calls/{callId}/analysis")
    ApiResponse<CallTranscriptStartResponse> requestAnalysis(
            @AuthUser final Long userId,
            @Parameter(description = "통화 ID", example = "42")
            @PathVariable final Long callId
    );

    @Operation(
            summary = "통화 분석 결과 조회",
            description = "비동기로 생성된 LLM 분석 결과를 본인 화자 기준으로 조회한다."
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
