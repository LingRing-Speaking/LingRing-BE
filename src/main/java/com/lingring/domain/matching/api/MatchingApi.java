package com.lingring.domain.matching.api;

import com.lingring.domain.matching.dto.response.MatchingStatusResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Matching", description = "사용자 매칭 대기열 API")
public interface MatchingApi {

    @Operation(
            summary = "매칭 대기열 입장",
            description = "인증된 사용자를 매칭 대기열에 적재한다. 매칭 자체는 백그라운드 워커가 주기적으로 수행하므로, "
                    + "결과는 GET /api/v1/me/matching 폴링으로 확인한다. 이미 큐에 있어도 입장 시각이 갱신되며 (멱등) 이전 매칭 결과는 클리어된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "대기열 입장 성공"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/me/matching")
    ApiResponse<Void> enterQueue(
            @AuthUser final Long userId
    );

    @Operation(
            summary = "매칭 상태 조회 (폴링)",
            description = "인증된 사용자의 현재 매칭 상태를 반환한다. MATCHED(상대 partnerId 포함), WAITING(아직 매칭 안 됨), NONE(대기열에도 없고 매칭 결과도 없음 → 클라이언트는 재입장 판단)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me/matching")
    ApiResponse<MatchingStatusResponse> getStatus(
            @AuthUser final Long userId
    );

    @Operation(
            summary = "매칭 대기열 취소",
            description = "인증된 사용자를 대기열에서 제거한다. 대기열에 없어도 204 (멱등)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "취소 성공 (대기열 미존재 포함)"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/me/matching")
    ApiResponse<Void> leaveQueue(
            @AuthUser final Long userId
    );

    @Operation(
            summary = "매칭 수락",
            description = "AWAITING_CONFIRM 상태의 매칭을 수락한다. 양쪽 모두 수락하면 MATCHED로 전이되어 roomId가 발급되며 통화 기록이 생성된다. "
                    + "수락 후에는 GET /api/v1/me/matching 폴링으로 결과를 확인한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "수락 처리됨 (양쪽 수락 완료 여부와 무관하게 204)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "수락할 매칭 컨펌 레코드가 없습니다 (이미 만료/decline 처리된 경우)"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/me/matching/accept")
    ApiResponse<Void> acceptMatch(
            @AuthUser final Long userId
    );

    @Operation(
            summary = "매칭 거절",
            description = "AWAITING_CONFIRM 상태의 매칭을 거절한다. 양쪽 모두 WAITING으로 복귀하며 페어 cooldown(10분)이 적용된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "거절 처리됨"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "거절할 매칭 컨펌 레코드가 없습니다"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/me/matching/decline")
    ApiResponse<Void> declineMatch(
            @AuthUser final Long userId
    );
}
