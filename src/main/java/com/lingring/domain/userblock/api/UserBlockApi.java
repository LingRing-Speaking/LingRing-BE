package com.lingring.domain.userblock.api;

import com.lingring.domain.userblock.dto.request.UserBlockCreateRequest;
import com.lingring.domain.userblock.dto.response.UserBlockListResponse;
import com.lingring.domain.userblock.dto.response.UserBlockResponse;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "UserBlock", description = "사용자 차단 API")
public interface UserBlockApi {

    @Operation(
            summary = "사용자 차단",
            description = "userId의 사용자가 blockedUserId의 사용자를 차단한다. 이미 차단된 경우 기존 차단 정보를 반환한다 (멱등)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "차단 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/BadRequest"
            )
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/users/{userId}/blocks")
    ApiResponse<UserBlockResponse> block(
            @Parameter(description = "차단을 요청하는 사용자 id", example = "1")
            @PathVariable("userId") final Long userId,
            @Valid @RequestBody final UserBlockCreateRequest request
    );

    @Operation(
            summary = "사용자 차단 해제",
            description = "userId의 사용자가 blockedUserId 사용자에 대한 차단을 해제한다. 차단 기록이 없어도 204를 반환한다 (멱등)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "차단 해제 성공 (대상 없음 포함)"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/users/{userId}/blocks/{blockedUserId}")
    ApiResponse<Void> unblock(
            @Parameter(description = "차단 해제를 요청하는 사용자 id", example = "1")
            @PathVariable("userId") final Long userId,
            @Parameter(description = "차단 해제 대상 사용자 id", example = "2")
            @PathVariable("blockedUserId") final Long blockedUserId
    );

    @Operation(
            summary = "차단한 사용자 목록 조회",
            description = "userId의 사용자가 차단한 목록을 최근 차단순(created_at DESC)으로 반환한다. 무한 스크롤용 Slice. size는 서버에서 최대 50으로 clamp된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users/{userId}/blocks")
    ApiResponse<UserBlockListResponse> getAll(
            @Parameter(description = "조회할 사용자 id", example = "1")
            @PathVariable("userId") final Long userId,
            @Parameter(description = "0-based 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") final int page,
            @Parameter(description = "페이지 크기 (1 ~ 50, 기본 20)", example = "20")
            @RequestParam(defaultValue = "20") final int size
    );
}
