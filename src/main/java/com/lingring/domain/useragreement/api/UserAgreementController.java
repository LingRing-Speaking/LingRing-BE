package com.lingring.domain.useragreement.api;

import com.lingring.domain.useragreement.dto.request.AgreementCreateRequest;
import com.lingring.domain.useragreement.dto.response.AgreementResponse;
import com.lingring.domain.useragreement.service.UserAgreementService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserAgreementController implements UserAgreementApi {

    private final UserAgreementService userAgreementService;

    @Override
    public ApiResponse<AgreementResponse> accept(
            final Long userId,
            final AgreementCreateRequest request
    ) {
        return ApiResponse.success(HttpStatus.OK, userAgreementService.accept(userId, request));
    }
}
