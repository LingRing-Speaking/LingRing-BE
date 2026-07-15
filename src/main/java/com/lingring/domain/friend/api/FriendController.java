package com.lingring.domain.friend.api;

import com.lingring.domain.friend.domain.FriendRequestDirection;
import com.lingring.domain.friend.domain.FriendshipStatus;
import com.lingring.domain.friend.dto.request.FriendRequestCreateRequest;
import com.lingring.domain.friend.dto.request.FriendshipUpdateRequest;
import com.lingring.domain.friend.dto.response.FriendSearchResponse;
import com.lingring.domain.friend.dto.response.FriendsResponse;
import com.lingring.domain.friend.dto.response.FriendshipResponse;
import com.lingring.domain.friend.dto.response.ReceivedCountResponse;
import com.lingring.domain.friend.service.FriendService;
import com.lingring.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FriendController implements FriendApi {

    private final FriendService friendService;

    @Override
    public ApiResponse<FriendshipResponse> sendRequest(
            final Long userId,
            final FriendRequestCreateRequest request
    ) {
        return ApiResponse.success(HttpStatus.CREATED, friendService.sendRequest(userId, request));
    }

    @Override
    public ApiResponse<FriendshipResponse> accept(
            final Long userId,
            final Long requesterId,
            final FriendshipUpdateRequest request
    ) {
        return ApiResponse.success(HttpStatus.OK, friendService.accept(userId, requesterId, request));
    }

    @Override
    public ApiResponse<Void> remove(final Long userId, final Long targetUserId) {
        friendService.remove(userId, targetUserId);
        return ApiResponse.success(HttpStatus.NO_CONTENT);
    }

    @Override
    public ApiResponse<FriendsResponse> getFriends(
            final Long userId,
            final FriendshipStatus status,
            final FriendRequestDirection direction,
            final int page,
            final int size
    ) {
        return ApiResponse.success(HttpStatus.OK, friendService.getFriends(userId, status, direction, page, size));
    }

    @Override
    public ApiResponse<FriendSearchResponse> search(final Long userId, final String nickname) {
        return ApiResponse.success(HttpStatus.OK, friendService.search(userId, nickname).orElse(null));
    }

    @Override
    public ApiResponse<ReceivedCountResponse> receivedCount(final Long userId) {
        return ApiResponse.success(HttpStatus.OK, friendService.receivedRequestCount(userId));
    }
}
