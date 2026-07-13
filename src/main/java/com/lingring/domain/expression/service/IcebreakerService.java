package com.lingring.domain.expression.service;

import com.lingring.domain.expression.dao.IcebreakerRepository;
import com.lingring.domain.expression.dao.UserExpressionRepository;
import com.lingring.domain.expression.domain.BookmarkSource;
import com.lingring.domain.expression.domain.Icebreaker;
import com.lingring.domain.expression.domain.UserExpression;
import com.lingring.domain.expression.dto.response.IcebreakerListResponse;
import com.lingring.global.common.pagination.PageSize;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IcebreakerService {

    private final IcebreakerRepository icebreakerRepository;
    private final UserExpressionRepository userExpressionRepository;

    @Transactional(readOnly = true)
    public IcebreakerListResponse getRandom(final Long userId, final int count) {
        final PageSize pageSize = PageSize.clamp(count);
        final List<Icebreaker> icebreakers = icebreakerRepository.findRandom(pageSize.value());
        if (icebreakers.isEmpty()) {
            throw new NotFoundException(
                    ErrorCode.ICEBREAKER_NOT_FOUND,
                    "등록된 아이스브레이커가 없습니다."
            );
        }
        return IcebreakerListResponse.from(icebreakers, findBookmarkIds(userId, icebreakers));
    }

    /** 호출자가 찜한 아이스브레이커의 (icebreakerId → 저장 표현 row id) 매핑. */
    private Map<Long, Long> findBookmarkIds(final Long userId, final List<Icebreaker> icebreakers) {
        final List<Long> icebreakerIds = icebreakers.stream().map(Icebreaker::getId).toList();
        return userExpressionRepository
                .findAllByUserIdAndSourceAndSourceRefIdIn(userId, BookmarkSource.ICEBREAKER, icebreakerIds)
                .stream()
                .collect(Collectors.toMap(UserExpression::getSourceRefId, UserExpression::getId));
    }
}
