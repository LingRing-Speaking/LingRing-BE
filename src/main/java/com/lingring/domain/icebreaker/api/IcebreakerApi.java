package com.lingring.domain.icebreaker.api;

import com.lingring.domain.icebreaker.dto.response.IcebreakerListResponse;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Icebreaker", description = "아이스브레이커(대화 시작 문장) API")
public interface IcebreakerApi {

    @Operation(
            summary = "아이스브레이커 무작위 N개 조회",
            description = "대화 상대에게 친숙하게 먼저 말을 걸 때 사용할 문장을 무작위로 N개 반환한다. "
                    + "count 파라미터로 개수를 지정한다 (기본 5, 1~50 범위로 clamp). "
                    + "호출할 때마다 결과가 달라진다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    ref = "#/components/responses/NotFound"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/icebreakers")
    ApiResponse<IcebreakerListResponse> getRandom(
            @RequestParam(name = "count", defaultValue = "5") int count
    );
}
