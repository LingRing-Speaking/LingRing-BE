package com.lingring.domain.referral.api;

import com.lingring.domain.referral.dto.request.RedeemReferralRequest;
import com.lingring.domain.referral.dto.response.RedeemReferralResponse;
import com.lingring.domain.referral.dto.response.ReferralStatusResponse;
import com.lingring.domain.referral.facade.ReferralFacade;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReferralController implements ReferralApi {

    private final ReferralFacade referralFacade;

    @Override
    public ApiResponse<ReferralStatusResponse> getStatus(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, referralFacade.getStatus(userId));
    }

    @Override
    public ApiResponse<RedeemReferralResponse> redeem(final Long userId, final RedeemReferralRequest request) {
        return ApiResponse.success(HttpStatus.OK, referralFacade.redeem(userId, request.nickname()));
    }
}
