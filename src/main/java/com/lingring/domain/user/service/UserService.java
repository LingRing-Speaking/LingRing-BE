package com.lingring.domain.user.service;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.dto.response.UserMyResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserMyResponse getMy(final Long userId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_NOT_FOUND,
                        "ID가 %d인 사용자를 찾을 수 없습니다.".formatted(userId)
                ));
        return UserMyResponse.from(user);
    }
}
