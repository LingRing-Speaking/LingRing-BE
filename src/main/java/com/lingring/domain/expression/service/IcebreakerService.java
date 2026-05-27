package com.lingring.domain.expression.service;

import com.lingring.domain.expression.dao.IcebreakerRepository;
import com.lingring.domain.expression.domain.Icebreaker;
import com.lingring.domain.expression.dto.response.IcebreakerListResponse;
import com.lingring.global.common.pagination.PageSize;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IcebreakerService {

    private final IcebreakerRepository icebreakerRepository;

    @Transactional(readOnly = true)
    public IcebreakerListResponse getRandom(final int count) {
        final PageSize pageSize = PageSize.clamp(count);
        final List<Icebreaker> icebreakers = icebreakerRepository.findRandom(pageSize.value());
        if (icebreakers.isEmpty()) {
            throw new NotFoundException(
                    ErrorCode.ICEBREAKER_NOT_FOUND,
                    "등록된 아이스브레이커가 없습니다."
            );
        }
        return IcebreakerListResponse.from(icebreakers);
    }
}
