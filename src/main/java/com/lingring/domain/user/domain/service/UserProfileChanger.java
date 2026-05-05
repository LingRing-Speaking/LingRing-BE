package com.lingring.domain.user.domain.service;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.domain.vo.ProfileImage;
import com.lingring.domain.user.domain.vo.ProfileImageKey;
import com.lingring.domain.user.exception.NicknameConflictException;
import com.lingring.domain.user.service.ProfileImageStorage;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileChanger {

    private final UserRepository userRepository;
    private final ProfileImageStorage profileImageStorage;

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
        user.changeProfileImage(new ProfileImage(profileImageStorage.publicUrl(key.getValue())));
    }

    private boolean isPresent(final String value) {
        return value != null && !value.isBlank();
    }
}
