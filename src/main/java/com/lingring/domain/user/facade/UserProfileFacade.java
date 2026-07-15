package com.lingring.domain.user.facade;

import com.lingring.domain.friend.domain.FriendRelation;
import com.lingring.domain.friend.service.FriendService;
import com.lingring.domain.user.dao.dto.UserProfileProjection;
import com.lingring.domain.user.dto.response.UserProfileResponse;
import com.lingring.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileFacade {

    private final UserService userService;
    private final FriendService friendService;

    public UserProfileResponse getUserProfile(final Long userId, final Long targetUserId) {
        final UserProfileProjection profile = userService.getUserProfile(targetUserId);
        final FriendRelation relation = friendService.getRelation(userId, targetUserId);
        return UserProfileResponse.of(profile, relation);
    }
}
