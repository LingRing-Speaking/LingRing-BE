package com.lingring.domain.user.domain.service;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.domain.vo.ProfileImage;
import com.lingring.domain.user.domain.vo.ProfileImageKey;
import com.lingring.domain.user.exception.NicknameConflictException;
import com.lingring.domain.user.service.ModerationVerdict;
import com.lingring.domain.user.service.ProfileImageModerator;
import com.lingring.domain.user.service.ProfileImageStorage;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileChanger {

    private final UserRepository userRepository;
    private final ProfileImageStorage profileImageStorage;
    private final ProfileImageModerator profileImageModerator;

    public void applyUpdates(
            final User user,
            final Long userId,
            final String nickname,
            final String profileImageKey
    ) {
        if (isPresent(nickname)) {
            changeNickname(user, nickname);
        }
        if (isPresent(profileImageKey)) {
            changeProfileImage(user, userId, profileImageKey);
        }
    }

    public void requireUniqueName(final Name name) {
        if (userRepository.existsByName(name)) {
            throw new NicknameConflictException(name.getValue());
        }
    }

    private void changeNickname(final User user, final String nickname) {
        final Name newName = new Name(nickname);
        requireUniqueName(newName);
        user.changeName(newName);
    }

    private void changeProfileImage(final User user, final Long userId, final String rawKey) {
        final ProfileImageKey key = ProfileImageKey.from(rawKey);
        key.requireOwnedBy(userId);
        if (!profileImageStorage.exists(key.getValue())) {
            throw new BadRequestException(
                    ErrorCode.IMAGE_NOT_UPLOADED,
                    "업로드되지 않은 이미지 키입니다: %s".formatted(key.getValue())
            );
        }
        final ModerationVerdict verdict = profileImageModerator.moderate(key.getValue());
        if (verdict.inappropriate()) {
            profileImageStorage.delete(key.getValue());
            log.warn("프로필 이미지 차단: userId={}, key={}, reasons={}",
                    userId, key.getValue(), verdict.reasons());
            throw new BadRequestException(
                    ErrorCode.INAPPROPRIATE_PROFILE_IMAGE,
                    "프로필 이미지 차단: userId=%d, key=%s, reasons=%s"
                            .formatted(userId, key.getValue(), verdict.reasons())
            );
        }
        user.changeProfileImage(new ProfileImage(profileImageStorage.publicUrl(key.getValue())));
    }

    private boolean isPresent(final String value) {
        return value != null && !value.isBlank();
    }
}
