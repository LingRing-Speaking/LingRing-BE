package com.lingring.domain.matching.api;

import com.lingring.domain.matching.dto.request.CallInvitationCreateRequest;
import com.lingring.domain.matching.dto.response.CallInvitationAcceptResponse;
import com.lingring.domain.matching.dto.response.CallInvitationStatusResponse;
import com.lingring.domain.matching.service.CallInvitationService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CallInvitationController implements CallInvitationApi {

    private final CallInvitationService callInvitationService;

    @Override
    public ApiResponse<Void> invite(final Long userId, final CallInvitationCreateRequest request) {
        callInvitationService.invite(userId, request.inviteeUserId());
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }

    @Override
    public ApiResponse<CallInvitationStatusResponse> getOutgoingStatus(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, callInvitationService.getOutgoingStatus(userId));
    }

    @Override
    public ApiResponse<Void> cancel(final Long userId) {
        callInvitationService.cancel(userId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }

    @Override
    public ApiResponse<CallInvitationAcceptResponse> accept(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, callInvitationService.accept(userId));
    }

    @Override
    public ApiResponse<Void> decline(final Long userId) {
        callInvitationService.decline(userId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }
}
