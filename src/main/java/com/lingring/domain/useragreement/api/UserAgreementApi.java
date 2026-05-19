package com.lingring.domain.useragreement.api;

import com.lingring.domain.useragreement.dto.request.AgreementCreateRequest;
import com.lingring.domain.useragreement.dto.response.AgreementResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "UserAgreement", description = "사용자 약관 동의 API")
public interface UserAgreementApi {

    @Operation(
            summary = "약관 동의 처리",
            description = """
                    가입 직후 약관 동의 화면에서 사용자가 필수 3개 항목(OVER14, TERMS, PRIVACY)에
                    동의했음을 기록한다. 동의 시점·약관 버전이 User에 저장되며, 이후 응답의
                    requiresOnboarding이 false로 전환된다.
                    (VOICE_AI는 enum에는 남아 있지만 현재 필수 항목에서 임시 제외됨)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "동의 처리 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 동의 항목 누락 또는 약관 버전 오류"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    ref = "#/components/responses/Unauthorized"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/me/agreements")
    ApiResponse<AgreementResponse> accept(
            @AuthUser final Long userId,
            @Valid @RequestBody final AgreementCreateRequest request
    );
}
