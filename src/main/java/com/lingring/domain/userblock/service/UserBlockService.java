package com.lingring.domain.userblock.service;

import com.lingring.domain.userblock.dao.UserBlockRepository;
import com.lingring.domain.userblock.domain.UserBlock;
import com.lingring.domain.userblock.dto.request.UserBlockCreateRequest;
import com.lingring.domain.userblock.dto.response.UserBlockListResponse;
import com.lingring.domain.userblock.dto.response.UserBlockResponse;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserBlockService {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 50;

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

    public UserBlockListResponse getAllByUserId(final Long userId, final int page, final int size) {
        final int clampedSize = Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);
        final Slice<UserBlock> slice = userBlockRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, clampedSize));
        return UserBlockListResponse.from(slice);
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
