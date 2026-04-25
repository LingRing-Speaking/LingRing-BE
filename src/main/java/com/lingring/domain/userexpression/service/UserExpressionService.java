package com.lingring.domain.userexpression.service;

import com.lingring.domain.userexpression.dao.UserExpressionRepository;
import com.lingring.domain.userexpression.domain.UserExpression;
import com.lingring.domain.userexpression.dto.request.UserExpressionCreateRequest;
import com.lingring.domain.userexpression.dto.response.UserExpressionListResponse;
import com.lingring.domain.userexpression.dto.response.UserExpressionResponse;
import com.lingring.domain.user.dao.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserExpressionService {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 50;

    private final UserExpressionRepository userExpressionRepository;
    private final UserStatsRepository userStatsRepository;

    @Transactional
    public UserExpressionResponse save(final Long userId, final UserExpressionCreateRequest request) {
        final UserExpression saved = userExpressionRepository.save(
                UserExpression.create(userId, request.expression(), request.meaning())
        );
        userStatsRepository.incrementExpressionCount(userId);
        return UserExpressionResponse.from(saved);
    }

    public UserExpressionListResponse getAllByUserId(final Long userId, final int page, final int size) {
        final int clampedSize = Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);
        final Slice<UserExpression> slice = userExpressionRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, clampedSize));
        return UserExpressionListResponse.from(slice);
    }

    @Transactional
    public void delete(final Long userId, final Long id) {
        userExpressionRepository.findByIdAndUserId(id, userId)
                .ifPresent(saved -> {
                    userExpressionRepository.delete(saved);
                    userStatsRepository.decrementExpressionCount(userId);
                });
    }
}
