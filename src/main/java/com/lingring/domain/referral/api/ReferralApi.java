package com.lingring.domain.referral.api;

import com.lingring.domain.referral.dto.request.RedeemReferralRequest;
import com.lingring.domain.referral.dto.response.RedeemReferralResponse;
import com.lingring.domain.referral.dto.response.ReferralStatusResponse;
import com.lingring.global.auth.annotation.AuthUser;
import com.lingring.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Referral", description = "추천인 입력 API")
public interface ReferralApi {

    @Operation(
            summary = "내 추천인 입력 가능 여부 조회",
            description = """
                    redeemable은 아직 입력하지 않았고, 가입 후 7일 이내이며, 재가입자가 아닐 때 true다.
                    redeemableUntil(가입 시각 + 7일)은 기간이 지나도 항상 반환한다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/me/referral")
    ApiResponse<ReferralStatusResponse> getStatus(@AuthUser final Long userId);

    @Operation(
            summary = "추천인 닉네임 입력",
            description = """
                    추천인의 닉네임(대소문자 무관)을 입력하면 입력자와 추천인에게 황금티켓을 3장씩 지급한다.
                    응답의 paidTicket은 지급 후 입력자의 황금티켓 수다. 실패 사유는 에러 응답의 code로 구분한다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "지급 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = """
                            code=REFERRAL_PERIOD_EXPIRED: 가입 후 7일 경과
                            code=REFERRAL_SELF_NOT_ALLOWED: 자기 닉네임 입력
                            """
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "code=REFERRAL_NOT_ELIGIBLE_REJOINED: 탈퇴 이력이 있는 재가입자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "code=REFERRER_NOT_FOUND: 해당 닉네임의 사용자 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "code=REFERRAL_ALREADY_REDEEMED: 이미 추천인을 입력함"
            )
    })
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/me/referral/redeem")
    ApiResponse<RedeemReferralResponse> redeem(
            @AuthUser final Long userId,
            @Valid @RequestBody final RedeemReferralRequest request
    );
}
