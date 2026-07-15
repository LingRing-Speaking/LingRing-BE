package com.lingring.domain.friend.dao.dto;

import com.lingring.domain.friend.domain.FriendshipStatus;
import java.time.LocalDateTime;

public interface FriendItemProjection {

    Long getUserId();

    String getNickname();

    String getProfileImage();

    FriendshipStatus getStatus();

    String getDirection();

    LocalDateTime getRequestedAt();
}
