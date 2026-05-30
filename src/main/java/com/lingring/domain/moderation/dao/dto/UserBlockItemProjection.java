package com.lingring.domain.moderation.dao.dto;

import java.time.LocalDateTime;

public interface UserBlockItemProjection {

    Long getId();

    Long getBlockedUserId();

    String getNickname();

    String getProfileImage();

    LocalDateTime getCreatedAt();
}
