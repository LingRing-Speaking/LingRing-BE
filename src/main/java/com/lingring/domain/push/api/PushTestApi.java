package com.lingring.domain.push.api;

import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "PushTest", description = "개발자용 푸시 테스트 발송 API")
public interface PushTestApi {

    @Operation(
            summary = "푸시 테스트 발송 (본인 기기)",
            description = """
                    BE 발송 경로(FCM 자격증명·payload·무효 토큰 정리) 확인용. 로그인한 본인 소유 토큰 전부로
                    DAILY_REMINDER와 같은 payload를 즉시 보낸다. 마케팅 푸시 동의 여부는 보지 않으며,
                    등록된 토큰이 없으면 아무것도 보내지 않는다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "발송 요청 완료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/me/push/test")
    ApiResponse<Void> sendTest(
            @AuthUser final Long userId
    );
}
