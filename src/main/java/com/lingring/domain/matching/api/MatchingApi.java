package com.lingring.domain.matching.api;

import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Matching", description = "사용자 매칭 대기열 API")
public interface MatchingApi {

    @Operation(
            summary = "매칭 대기열 입장",
            description = "userId의 사용자가 매칭 대기열에 입장한다. 즉시 매칭 가능한 상대가 있으면 MATCHED와 partnerId를, 없으면 WAITING을 반환한다. "
                    + "이미 큐에 있는 상태에서 다시 호출하면 입장 시각이 갱신되고 새 후보 탐색을 다시 시도한다 (멱등)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "대기 중 또는 즉시 매칭 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/users/{userId}/matching")
    ApiResponse<MatchingStatusResponse> enterQueue(
            @Parameter(description = "매칭을 요청하는 사용자 id", example = "1")
            @PathVariable("userId") final Long userId
    );

    @Operation(
            summary = "매칭 상태 조회 (폴링)",
            description = "userId의 사용자에 대해 현재 매칭 상태를 반환한다. MATCHED(상대 partnerId 포함), WAITING(아직 매칭 안 됨), NONE(대기열에도 없고 매칭 결과도 없음 → 클라이언트는 재입장 판단)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users/{userId}/matching")
    ApiResponse<MatchingStatusResponse> getStatus(
            @Parameter(description = "조회할 사용자 id", example = "1")
            @PathVariable("userId") final Long userId
    );

    @Operation(
            summary = "매칭 대기열 취소",
            description = "userId의 사용자를 대기열에서 제거한다. 대기열에 없어도 204 (멱등)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "취소 성공 (대기열 미존재 포함)"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/users/{userId}/matching")
    ApiResponse<Void> leaveQueue(
            @Parameter(description = "취소를 요청하는 사용자 id", example = "1")
            @PathVariable("userId") final Long userId
    );
}
