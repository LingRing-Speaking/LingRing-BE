package com.lingring.domain.review.api;

import com.lingring.domain.review.dto.response.CallTranscriptResponse;
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

@Tag(name = "CallTranscript", description = "통화 transcript 조회 API")
public interface CallTranscriptApi {

    @Operation(
            summary = "통화 transcript 조회",
            description = "분석이 완료된 통화의 transcript(발화 세그먼트)를 조회한다."
                    + " segments는 startSec 오름차순으로 정렬되어 반환된다."
                    + " 통화 참여자 본인만 조회할 수 있으며, 참여자가 아니면 403,"
                    + " transcript record가 없으면 404, STT가 아직 완료되지 않았으면 409를 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/calls/{callId}/transcript")
    ApiResponse<CallTranscriptResponse> getTranscript(
            @AuthUser final Long userId,
            @Parameter(description = "통화 ID", example = "42")
            @PathVariable final Long callId
    );
}
