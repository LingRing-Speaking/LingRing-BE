package com.lingring.domain.expression.api;

import com.lingring.domain.expression.dto.request.BookmarkCreateRequest;
import com.lingring.domain.expression.dto.response.UserExpressionListResponse;
import com.lingring.domain.expression.dto.response.UserExpressionResponse;
import com.lingring.global.auth.annotation.AuthUser;
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

@Tag(name = "UserExpression", description = "저장한 표현 API")
public interface UserExpressionApi {

    @Operation(
            summary = "표현 찜(북마크) 생성",
            description = """
                    인증된 사용자가 소스(분석 mistake / 오늘의 추천 표현 / 아이스브레이커)를 지정해
                    표현을 찜한다. body는 source 필드로 갈리는 discriminated union이며, 표현/뜻
                    텍스트는 서버가 소스에서 채운다. 같은 소스를 다시 찜하면 새 row 없이 기존
                    표현을 반환한다 (멱등)."""
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "생성 성공 (이미 찜한 소스면 기존 표현 반환)",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    ref = "#/components/responses/BadRequest"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "남의 분석의 mistake를 찜하려는 경우"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "지정한 소스(분석/mistake/추천 표현/아이스브레이커)가 없는 경우"
            )
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/expressions")
    ApiResponse<UserExpressionResponse> create(
            @AuthUser final Long userId,
            @Valid @RequestBody final BookmarkCreateRequest request
    );

    @Operation(
            summary = "저장한 표현 목록 조회",
            description = "인증된 사용자가 저장한 표현을 최근 저장순(created_at DESC)으로 반환한다. 무한 스크롤용 Slice. size는 서버에서 최대 50으로 clamp된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/expressions")
    ApiResponse<UserExpressionListResponse> getAll(
            @AuthUser final Long userId,
            @Parameter(description = "0-based 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") final int page,
            @Parameter(description = "페이지 크기 (1 ~ 50, 기본 20)", example = "20")
            @RequestParam(defaultValue = "20") final int size
    );

    @Operation(
            summary = "저장한 표현 삭제",
            description = "인증된 사용자가 자신의 expressionId에 해당하는 저장한 표현을 삭제한다. 대상이 없거나 본인 소유가 아니어도 204를 반환한다 (멱등)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "삭제 성공 (대상 없음 포함)"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/expressions/{expressionId}")
    ApiResponse<Void> delete(
            @AuthUser final Long userId,
            @Parameter(description = "삭제할 저장 표현 id", example = "1")
            @PathVariable("expressionId") final Long expressionId
    );
}
