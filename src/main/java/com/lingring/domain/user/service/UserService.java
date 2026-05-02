package com.lingring.domain.user.service;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.UserStats;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.dto.response.MeResponse;
import com.lingring.domain.user.dto.response.UserProfileResponse;
import com.lingring.domain.user.exception.NicknameConflictException;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.error.exception.UnauthorizedException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;

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
    public UserProfileResponse getUserProfile(final Long userId) {
        return userRepository.findProfileById(userId)
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "ID가 %d인 사용자를 찾을 수 없습니다.".formatted(userId)
                ));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByProvider(final Provider provider, final String providerUserId) {
        return userRepository.findByProviderAndProviderUserId(provider, providerUserId);
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
        if (userRepository.existsByName(name)) {
            throw new NicknameConflictException(nickname);
        }
        final User user = userRepository.save(
                User.createFromOAuth(provider, providerUserId, name, null)
        );
        userStatsRepository.save(UserStats.create(user.getId()));
        return user;
    }
}
