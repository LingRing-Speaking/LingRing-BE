package com.lingring.domain.user.service;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.dao.dto.UserProfileProjection;
import com.lingring.domain.user.domain.port.ProfileImageStorage;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.UserStats;
import com.lingring.domain.user.domain.policy.ProfileImagePolicy;
import com.lingring.domain.user.domain.service.UserProfileChanger;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.domain.vo.ProfileImageKey;
import com.lingring.domain.user.dto.request.PresignedUrlRequest;
import com.lingring.domain.user.dto.request.UpdateProfileRequest;
import com.lingring.domain.user.dto.response.MeResponse;
import com.lingring.domain.user.dto.response.PresignedUrlResponse;
import com.lingring.domain.user.dto.response.UpdateProfileResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.error.exception.UnauthorizedException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final ProfileImageStorage profileImageStorage;
    private final ProfileImagePolicy profileImagePolicy;
    private final UserProfileChanger userProfileChanger;

    @Transactional(readOnly = true)
    public MeResponse getMe(final Long userId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.INVALID_TOKEN,
                        "토큰 소유자를 찾을 수 없습니다. 다시 로그인하세요."
                ));
        return MeResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserProfileProjection getUserProfile(final Long userId) {
        return userRepository.findProfileById(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "ID가 %d인 사용자를 찾을 수 없습니다.".formatted(userId)
                ));
    }

    @Transactional(readOnly = true)
    public User getById(final Long userId) {
        return getUser(userId);
    }

    @Transactional
    public void delete(final User user) {
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByProvider(final Provider provider, final String providerUserId) {
        return userRepository.findByProviderAndProviderUserId(provider, providerUserId);
    }

    @Transactional
    public void updateAppleCredential(final Long userId, final String refreshToken) {
        final User user = getUser(userId);
        user.updateAppleCredential(refreshToken);
    }

    @Transactional
    public User register(
            final Provider provider,
            final String providerUserId,
            final String nickname
    ) {
        if (nickname == null || nickname.isBlank()) {
            throw new BadRequestException(
                    ErrorCode.NICKNAME_REQUIRED,
                    "신규 가입에는 nickname이 필요합니다."
            );
        }
        final Name name = new Name(nickname);
        userProfileChanger.requireUniqueName(name);
        final User user = userRepository.save(User.createFromOAuth(provider, providerUserId, name, null));
        userStatsRepository.save(UserStats.create(user.getId()));
        return user;
    }

    @Transactional(readOnly = true)
    public PresignedUrlResponse createProfileImageUploadUrl(
            final Long userId,
            final PresignedUrlRequest request
    ) {
        profileImagePolicy.requireAcceptable(request.contentType(), request.contentLength());
        final ProfileImageKey key = ProfileImageKey.generateFor(userId);
        return PresignedUrlResponse.from(profileImageStorage.generateUploadUrl(key.getValue(), request.contentType(), request.contentLength()));
    }

    @Transactional
    public UpdateProfileResponse updateProfile(
            final Long userId,
            final UpdateProfileRequest request
    ) {
        if (request.hasNoChanges()) {
            throw new BadRequestException(ErrorCode.EMPTY_UPDATE_PROFILE_REQUEST, "변경할 항목이 하나 이상 필요합니다.");
        }
        final User user = getUser(userId);
        userProfileChanger.applyUpdates(user, userId, request.nickname(), request.profileImageKey());
        return UpdateProfileResponse.from(user);
    }

    private @NonNull User getUser(final Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "ID가 %d인 사용자를 찾을 수 없습니다.".formatted(userId)
                ));
    }
}
