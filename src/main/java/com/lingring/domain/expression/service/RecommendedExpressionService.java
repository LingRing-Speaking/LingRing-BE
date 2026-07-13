package com.lingring.domain.expression.service;

import static org.springframework.data.domain.Sort.Direction.ASC;

import com.lingring.domain.expression.dao.RecommendedExpressionRepository;
import com.lingring.domain.expression.dao.UserExpressionRepository;
import com.lingring.domain.expression.domain.BookmarkSource;
import com.lingring.domain.expression.domain.RecommendedExpression;
import com.lingring.domain.expression.domain.UserExpression;
import com.lingring.domain.expression.dto.response.RecommendedExpressionResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import com.lingring.global.util.DateTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecommendedExpressionService {

    private final RecommendedExpressionRepository recommendedExpressionRepository;
    private final UserExpressionRepository userExpressionRepository;
    private final DateTimeProvider dateTimeProvider;

    @Transactional(readOnly = true)
    public RecommendedExpressionResponse getDaily(final Long userId) {
        final long count = recommendedExpressionRepository.count();
        if (count == 0L) {
            throw new NotFoundException(
                    ErrorCode.RECOMMENDED_EXPRESSION_NOT_FOUND,
                    "추천 표현이 등록되어 있지 않습니다."
            );
        }
        final long epochDay = dateTimeProvider.now().toLocalDate().toEpochDay();
        final int offset = (int) Math.floorMod(epochDay, count);

        final RecommendedExpression today = recommendedExpressionRepository
                .findAll(PageRequest.of(offset, 1, Sort.by(ASC, "id")))
                .getContent()
                .getFirst();
        final Long bookmarkId = userExpressionRepository
                .findByUserIdAndSourceAndSourceRefIdAndSourceSubIndex(
                        userId,
                        BookmarkSource.DAILY_EXPRESSION,
                        today.getId(),
                        UserExpression.SHARED_SOURCE_SUB_INDEX)
                .map(UserExpression::getId)
                .orElse(null);
        return RecommendedExpressionResponse.from(today, bookmarkId);
    }
}
