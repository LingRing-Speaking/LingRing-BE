package com.lingring.domain.user.service;

import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.domain.user.domain.UserStats;
import com.lingring.domain.user.dto.response.UserStatsResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final UserStatsRepository userStatsRepository;

    @Transactional(readOnly = true)
    public UserStatsResponse getByUserId(final Long userId) {
        final UserStats userStats = userStatsRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.USER_STATS_NOT_FOUND,
                        "userId가 %d인 사용자 통계를 찾을 수 없습니다.".formatted(userId)
                ));
        return UserStatsResponse.from(userStats);
    }
}
