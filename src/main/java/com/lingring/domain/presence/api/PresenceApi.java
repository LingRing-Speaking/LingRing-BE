package com.lingring.domain.presence.api;

import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Presence", description = "접속 상태(온라인/오프라인) API")
public interface PresenceApi {

    @Operation(
            summary = "접속 상태 하트비트",
            description = "인증된 사용자를 온라인 상태로 갱신한다. 클라이언트는 포그라운드 동안 5초 주기로 호출한다. "
                    + "10초(TTL) 안에 다음 하트비트가 없으면 자동으로 오프라인 처리된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "온라인 상태 갱신 성공"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/me/presence")
    ApiResponse<Void> heartbeat(
            @AuthUser final Long userId
    );

    @Operation(
            summary = "접속 상태 해제 (명시적 오프라인)",
            description = "인증된 사용자를 즉시 오프라인 상태로 전환한다. "
                    + "앱이 백그라운드로 전환되거나 로그아웃할 때 호출한다. TTL 만료를 기다리지 않고 즉시 반영된다(멱등)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "오프라인 전환 성공 (이미 오프라인 포함)"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/me/presence")
    ApiResponse<Void> disconnect(
            @AuthUser final Long userId
    );
}
