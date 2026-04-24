package com.lingring.domain.savedexpression.service;

import com.lingring.domain.savedexpression.dao.SavedExpressionRepository;
import com.lingring.domain.savedexpression.domain.SavedExpression;
import com.lingring.domain.savedexpression.dto.request.SavedExpressionCreateRequest;
import com.lingring.domain.savedexpression.dto.response.SavedExpressionListResponse;
import com.lingring.domain.savedexpression.dto.response.SavedExpressionResponse;
import com.lingring.domain.user.dao.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SavedExpressionService {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 50;

    private final SavedExpressionRepository savedExpressionRepository;
    private final UserStatsRepository userStatsRepository;

    @Transactional
    public SavedExpressionResponse save(final Long userId, final SavedExpressionCreateRequest request) {
        final SavedExpression saved = savedExpressionRepository.save(
                SavedExpression.create(userId, request.expression(), request.meaning())
        );
        userStatsRepository.incrementSavedExpressionCount(userId);
        return SavedExpressionResponse.from(saved);
    }

    public SavedExpressionListResponse getAllByUserId(final Long userId, final int page, final int size) {
        final int clampedSize = Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);
        final Slice<SavedExpression> slice = savedExpressionRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, clampedSize));
        return SavedExpressionListResponse.from(slice);
    }

    @Transactional
    public void delete(final Long userId, final Long id) {
        savedExpressionRepository.findByIdAndUserId(id, userId)
                .ifPresent(saved -> {
                    savedExpressionRepository.delete(saved);
                    userStatsRepository.decrementSavedExpressionCount(userId);
                });
    }
}
