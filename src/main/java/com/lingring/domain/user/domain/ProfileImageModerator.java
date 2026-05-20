package com.lingring.domain.user.domain;

public interface ProfileImageModerator {

    ModerationVerdict moderate(String key);
}
