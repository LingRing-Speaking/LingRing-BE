package com.lingring.domain.user.api;

import com.lingring.domain.user.dto.response.UserMyResponse;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "User", description = "사용자 API")
public interface UserApi {

    @Operation(
            summary = "사용자 본인 정보 조회",
            description = "사용자 id로 본인의 기본 정보(id, name)를 조회한다."
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
    @GetMapping("/users/{userId}/my")
    ApiResponse<UserMyResponse> getMy(
            @Parameter(description = "조회할 사용자 id", example = "1")
            @PathVariable("userId") final Long userId
    );
}
