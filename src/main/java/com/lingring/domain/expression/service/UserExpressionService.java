package com.lingring.domain.expression.service;

import com.lingring.domain.expression.dao.IcebreakerRepository;
import com.lingring.domain.expression.dao.RecommendedExpressionRepository;
import com.lingring.domain.expression.dao.UserExpressionRepository;
import com.lingring.domain.expression.domain.BookmarkSource;
import com.lingring.domain.expression.domain.Icebreaker;
import com.lingring.domain.expression.domain.RecommendedExpression;
import com.lingring.domain.expression.domain.UserExpression;
import com.lingring.domain.expression.dto.request.BookmarkCreateRequest;
import com.lingring.domain.expression.dto.response.UserExpressionListResponse;
import com.lingring.domain.expression.dto.response.UserExpressionResponse;
import com.lingring.domain.review.dao.CallAnalysisRepository;
import com.lingring.domain.review.domain.analysis.CallAnalysis;
import com.lingring.domain.review.domain.analysis.vo.MistakeItem;
import com.lingring.domain.review.exception.CallAnalysisAccessForbiddenException;
import com.lingring.domain.review.exception.CallAnalysisNotFoundException;
import com.lingring.domain.user.dao.UserStatsRepository;
import com.lingring.global.common.pagination.PageSize;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import com.lingring.global.error.exception.InvalidValueException;
import com.lingring.global.error.exception.NotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserExpressionService {

    private final UserExpressionRepository userExpressionRepository;
    private final UserStatsRepository userStatsRepository;
    private final IcebreakerRepository icebreakerRepository;
    private final RecommendedExpressionRepository recommendedExpressionRepository;
    private final CallAnalysisRepository callAnalysisRepository;

    private record BookmarkTarget(
            BookmarkSource source,
            Long sourceRefId,
            int sourceSubIndex,
            String expression,
            String meaning
    ) {
    }

    @Transactional
    public UserExpressionResponse save(final Long userId, final BookmarkCreateRequest request) {
        final BookmarkTarget target = resolve(userId, request);
        final Optional<UserExpression> existing = userExpressionRepository
                .findByUserIdAndSourceAndSourceRefIdAndSourceSubIndex(
                        userId, target.source(), target.sourceRefId(), target.sourceSubIndex());
        if (existing.isPresent()) {
            return UserExpressionResponse.from(existing.get());
        }
        final UserExpression saved = userExpressionRepository.save(UserExpression.bookmark(
                userId, target.expression(), target.meaning(),
                target.source(), target.sourceRefId(), target.sourceSubIndex()));
        userStatsRepository.incrementExpressionCount(userId);
        return UserExpressionResponse.from(saved);
    }

    private BookmarkTarget resolve(final Long userId, final BookmarkCreateRequest request) {
        if (request.source() == BookmarkSource.ANALYSIS_MISTAKE) {
            return resolveMistake(userId, request);
        }
        if (request.source() == BookmarkSource.DAILY_EXPRESSION) {
            return resolveDaily(request);
        }
        if (request.source() == BookmarkSource.ICEBREAKER) {
            return resolveIcebreaker(request);
        }
        throw new InvalidValueException(
                ErrorCode.INVALID_INPUT_VALUE,
                "지원하지 않는 찜 소스입니다: %s".formatted(request.source())
        );
    }

    private BookmarkTarget resolveMistake(final Long userId, final BookmarkCreateRequest request) {
        if (request.analysisId() == null || request.mistakeId() == null) {
            throw new BadRequestException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "ANALYSIS_MISTAKE 찜에는 analysisId와 mistakeId가 필요합니다."
            );
        }
        final CallAnalysis analysis = callAnalysisRepository.findById(request.analysisId())
                .orElseThrow(() -> new CallAnalysisNotFoundException(request.analysisId()));
        if (!analysis.isOwnedBy(userId)) {
            throw new CallAnalysisAccessForbiddenException(request.analysisId(), userId);
        }
        final MistakeItem mistake = analysis.findMistake(request.mistakeId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.ANALYSIS_MISTAKE_NOT_FOUND,
                        "analysisId=%d 의 결과에서 mistakeId=%d 를 찾을 수 없습니다."
                                .formatted(request.analysisId(), request.mistakeId())
                ));
        return new BookmarkTarget(
                BookmarkSource.ANALYSIS_MISTAKE,
                request.analysisId(),
                request.mistakeId(),
                mistake.improved(),
                mistake.koMeaning()
        );
    }

    private BookmarkTarget resolveDaily(final BookmarkCreateRequest request) {
        if (request.recommendedExpressionId() == null) {
            throw new BadRequestException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "DAILY_EXPRESSION 찜에는 recommendedExpressionId가 필요합니다."
            );
        }
        final RecommendedExpression daily = recommendedExpressionRepository
                .findById(request.recommendedExpressionId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.RECOMMENDED_EXPRESSION_NOT_FOUND,
                        "recommendedExpressionId=%d 인 추천 표현을 찾을 수 없습니다."
                                .formatted(request.recommendedExpressionId())
                ));
        return new BookmarkTarget(
                BookmarkSource.DAILY_EXPRESSION,
                daily.getId(),
                UserExpression.SHARED_SOURCE_SUB_INDEX,
                daily.getExpression().getValue(),
                daily.getMeaning().getValue()
        );
    }

    private BookmarkTarget resolveIcebreaker(final BookmarkCreateRequest request) {
        if (request.icebreakerId() == null) {
            throw new BadRequestException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "ICEBREAKER 찜에는 icebreakerId가 필요합니다."
            );
        }
        final Icebreaker icebreaker = icebreakerRepository.findById(request.icebreakerId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.ICEBREAKER_NOT_FOUND,
                        "icebreakerId=%d 인 아이스브레이커를 찾을 수 없습니다."
                                .formatted(request.icebreakerId())
                ));
        return new BookmarkTarget(
                BookmarkSource.ICEBREAKER,
                icebreaker.getId(),
                UserExpression.SHARED_SOURCE_SUB_INDEX,
                icebreaker.getExpression().getValue(),
                icebreaker.getMeaning().getValue()
        );
    }

    @Transactional(readOnly = true)
    public UserExpressionListResponse getAllByUserId(final Long userId, final int page, final int size) {
        final PageSize pageSize = PageSize.clamp(size);
        final Slice<UserExpression> slice = userExpressionRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, pageSize.value()));
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
