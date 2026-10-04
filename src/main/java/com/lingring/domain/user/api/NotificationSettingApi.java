package com.lingring.domain.user.api;

import com.lingring.domain.user.dto.request.NotificationSettingUpdateRequest;
import com.lingring.domain.user.dto.response.NotificationSettingResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "NotificationSetting", description = "알림 수신 설정 API")
public interface NotificationSettingApi {

    @Operation(
            summary = "마케팅 푸시 수신 동의 변경",
            description = """
                    설정 화면 토글로 마케팅 푸시 수신 동의(true) 또는 철회(false)를 처리한다.
                    같은 값을 다시 보내도 성공하며, 값이 바뀐 경우에만 marketingPushUpdatedAt을 갱신한다.
                    철회해도 디바이스 토큰은 삭제하지 않는다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "marketingPush 누락"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    ref = "#/components/responses/NotFound"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/me/notification-settings")
    ApiResponse<NotificationSettingResponse> update(
            @AuthUser final Long userId,
            @Valid @RequestBody final NotificationSettingUpdateRequest request
    );
}
