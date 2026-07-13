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
import com.lingring.global.error.exception.NotFoundException;
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

    /**
     * 소스에서 도출된 찜 대상. expression/meaning은 서버가 채우고, 클라이언트가 보낸
     * 텍스트는 신뢰하지 않는다.
     */
    private record BookmarkTarget(
            BookmarkSource source,
            Long sourceRefId,
            int sourceSubIndex,
            String expression,
            String meaning
    ) {
    }

    /**
     * 소스 기반 찜(북마크) 생성. 같은 (user, source, refId, subIndex)가 이미 있으면
     * 새 row 없이 기존 표현을 반환한다(멱등) — expressionCount도 증가하지 않는다.
     */
    @Transactional
    public UserExpressionResponse save(final Long userId, final BookmarkCreateRequest request) {
        final BookmarkTarget target = resolve(userId, request);
        return userExpressionRepository
                .findByUserIdAndSourceAndSourceRefIdAndSourceSubIndex(
                        userId, target.source(), target.sourceRefId(), target.sourceSubIndex())
                .map(UserExpressionResponse::from)
                .orElseGet(() -> {
                    final UserExpression saved = userExpressionRepository.save(UserExpression.bookmark(
                            userId, target.expression(), target.meaning(),
                            target.source(), target.sourceRefId(), target.sourceSubIndex()));
                    userStatsRepository.incrementExpressionCount(userId);
                    return UserExpressionResponse.from(saved);
                });
    }

    private BookmarkTarget resolve(final Long userId, final BookmarkCreateRequest request) {
        return switch (request) {
            case BookmarkCreateRequest.AnalysisMistake r -> resolveMistake(userId, r);
            case BookmarkCreateRequest.DailyExpression r -> resolveDaily(r);
            case BookmarkCreateRequest.Icebreaker r -> resolveIcebreaker(r);
        };
    }

    private BookmarkTarget resolveMistake(final Long userId, final BookmarkCreateRequest.AnalysisMistake request) {
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

    private BookmarkTarget resolveDaily(final BookmarkCreateRequest.DailyExpression request) {
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

    private BookmarkTarget resolveIcebreaker(final BookmarkCreateRequest.Icebreaker request) {
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
