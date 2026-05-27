package com.lingring.domain.user.domain.port;

public interface ProfileImageModerator {

    ModerationVerdict moderate(String key);
}
