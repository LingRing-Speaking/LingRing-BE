package com.lingring.domain.user.service;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.dto.response.MeResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MeResponse getMe(final Long userId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCode.INVALID_TOKEN,
                        "토큰 소유자를 찾을 수 없습니다. 다시 로그인하세요."
                ));
        return MeResponse.from(user);
    }
}
