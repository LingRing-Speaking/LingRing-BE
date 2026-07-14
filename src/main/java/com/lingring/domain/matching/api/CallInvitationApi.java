package com.lingring.domain.matching.api;

import com.lingring.domain.matching.dto.request.CallInvitationCreateRequest;
import com.lingring.domain.matching.dto.response.CallInvitationAcceptResponse;
import com.lingring.domain.matching.dto.response.CallInvitationStatusResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Call Invitation", description = "친구 통화 초대(CallInvitation) API. 친구에게 직접 통화를 거는 플로우를 관리한다.")
public interface CallInvitationApi {

    @Operation(
            summary = "통화 초대 생성",
            description = "친구(ACCEPTED)이면서 온라인인 사용자에게 통화 초대를 보낸다. 초대는 30초(TTL) 후 자동 만료된다. "
                    + "수신자는 presence heartbeat 응답의 incomingInvitation으로 초대를 발견하고, "
                    + "발신자는 GET /api/v1/call-invitations/outgoing 폴링으로 결과를 확인한다. "
                    + "상대의 통화 중/랜덤 매칭 대기 여부는 검사하지 않는다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "초대 생성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "상대와 친구 관계(ACCEPTED)가 아닙니다"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "수신자 오프라인 / 이미 진행 중인 발신 초대 존재 / 수신자가 다른 초대를 받는 중"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/call-invitations")
    ApiResponse<Void> invite(
            @AuthUser final Long userId,
            @Valid @RequestBody final CallInvitationCreateRequest request
    );

    @Operation(
            summary = "발신 초대 상태 조회 (폴링)",
            description = "발신자가 '전화 거는 화면'에서 1초 주기로 폴링한다. "
                    + "RINGING(대기 중), ACCEPTED(수락됨 — roomId·callId 포함, /ws/signaling 접속), "
                    + "DECLINED(거절됨), NONE(만료·취소 등 진행 중인 초대 없음 — 발신 직후 문맥이면 무응답으로 해석)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/call-invitations/outgoing")
    ApiResponse<CallInvitationStatusResponse> getOutgoingStatus(
            @AuthUser final Long userId
    );

    @Operation(
            summary = "발신 초대 취소",
            description = "진행 중인 발신 초대를 취소한다. 수신자의 벨은 다음 heartbeat(≤5초)에서 멈춘다. 초대가 없어도 204 (멱등)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "취소 성공 (초대 미존재 포함)"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/call-invitations/outgoing")
    ApiResponse<Void> cancel(
            @AuthUser final Long userId
    );

    @Operation(
            summary = "수신 초대 수락",
            description = "수신 중인 통화 초대를 수락한다. 초대를 원자적으로 소비하고 통화(Call)를 생성하며, "
                    + "응답의 roomId·callId로 즉시 /ws/signaling에 접속한다. 발신자에게는 폴링으로 ACCEPTED가 전달된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수락 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "수락할 초대가 없습니다 (만료·취소된 경우)"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/call-invitations/accept")
    ApiResponse<CallInvitationAcceptResponse> accept(
            @AuthUser final Long userId
    );

    @Operation(
            summary = "수신 초대 거절",
            description = "수신 중인 통화 초대를 거절한다. 발신자에게는 폴링으로 DECLINED가 전달된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "거절 처리됨"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "거절할 초대가 없습니다 (만료·취소된 경우)"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/call-invitations/decline")
    ApiResponse<Void> decline(
            @AuthUser final Long userId
    );
}
