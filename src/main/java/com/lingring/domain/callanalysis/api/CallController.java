package com.lingring.domain.callanalysis.api;

import com.lingring.domain.callanalysis.dto.response.CallsResponse;
import com.lingring.domain.callanalysis.facade.CallHistoryFacade;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CallController implements CallApi {

    private final CallHistoryFacade callHistoryFacade;

    @Override
    public ApiResponse<CallsResponse> getAll(
            final Long userId,
            final int page,
            final int size
    ) {
        return ApiResponse.success(HttpStatus.OK, callHistoryFacade.getCallsByUserId(userId, page, size));
    }
}
