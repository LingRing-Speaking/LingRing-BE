package com.lingring.domain.userexpression.api;

import com.lingring.domain.userexpression.dto.request.UserExpressionCreateRequest;
import com.lingring.domain.userexpression.dto.response.UserExpressionListResponse;
import com.lingring.domain.userexpression.dto.response.UserExpressionResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "UserExpression", description = "저장한 표현 API")
public interface UserExpressionApi {

    @Operation(
            summary = "저장한 표현 생성",
            description = "userId의 사용자가 새로운 표현과 뜻을 저장한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/BadRequest"
            )
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/users/{userId}/expressions")
    ApiResponse<UserExpressionResponse> create(
            @Parameter(description = "저장할 사용자 id", example = "1")
            @PathVariable("userId") final Long userId,
            @RequestBody final UserExpressionCreateRequest request
    );

    @Operation(
            summary = "저장한 표현 목록 조회",
            description = "userId의 사용자가 저장한 표현을 최근 저장순(created_at DESC)으로 반환한다. 무한 스크롤용 Slice. size는 서버에서 최대 50으로 clamp된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/users/{userId}/expressions")
    ApiResponse<UserExpressionListResponse> getAll(
            @Parameter(description = "조회할 사용자 id", example = "1")
            @PathVariable("userId") final Long userId,
            @Parameter(description = "0-based 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") final int page,
            @Parameter(description = "페이지 크기 (1 ~ 50, 기본 20)", example = "20")
            @RequestParam(defaultValue = "20") final int size
    );

    @Operation(
            summary = "저장한 표현 삭제",
            description = "userId의 사용자가 자신의 id에 해당하는 저장한 표현을 삭제한다. 대상이 없거나 본인 소유가 아니어도 204를 반환한다 (멱등)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "삭제 성공 (대상 없음 포함)"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/users/{userId}/expressions/{id}")
    ApiResponse<Void> delete(
            @Parameter(description = "삭제 요청 사용자 id", example = "1")
            @PathVariable("userId") final Long userId,
            @Parameter(description = "삭제할 저장 표현 id", example = "1")
            @PathVariable("id") final Long id
    );
}
