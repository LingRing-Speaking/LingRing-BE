package com.lingring.domain.moderation.service;

import com.lingring.domain.moderation.dao.UserBlockRepository;
import com.lingring.domain.moderation.dao.dto.UserBlockItemProjection;
import com.lingring.domain.moderation.domain.UserBlock;
import com.lingring.domain.moderation.dto.request.UserBlockCreateRequest;
import com.lingring.domain.moderation.dto.response.UserBlockResponse;
import com.lingring.domain.moderation.dto.response.UserBlocksResponse;
import com.lingring.global.common.pagination.PageSize;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;

    @Transactional
    public UserBlockResponse block(final Long userId, final UserBlockCreateRequest request) {
        final Long blockedUserId = request.blockedUserId();
        validateNotSelfBlock(userId, blockedUserId);

        final UserBlock userBlock = userBlockRepository
                .findByUserIdAndBlockedUserId(userId, blockedUserId)
                .orElseGet(() -> userBlockRepository.save(UserBlock.create(userId, blockedUserId)));
        return UserBlockResponse.from(userBlock);
    }

    @Transactional
    public void unblock(final Long userId, final Long blockedUserId) {
        userBlockRepository.findByUserIdAndBlockedUserId(userId, blockedUserId)
                .ifPresent(userBlockRepository::delete);
    }

    @Transactional(readOnly = true)
    public UserBlocksResponse getAllByUserId(final Long userId, final int page, final int size) {
        final PageSize pageSize = PageSize.clamp(size);
        final Slice<UserBlockItemProjection> slice = userBlockRepository
                .findItemsByUserId(userId, PageRequest.of(page, pageSize.value()));
        return UserBlocksResponse.from(slice);
    }

    private void validateNotSelfBlock(final Long userId, final Long blockedUserId) {
        if (userId.equals(blockedUserId)) {
            throw new BadRequestException(
                    ErrorCode.SELF_BLOCK_NOT_ALLOWED,
                    "userId가 %d인 사용자가 자기 자신을 차단하려 했습니다.".formatted(userId)
            );
        }
    }
}
