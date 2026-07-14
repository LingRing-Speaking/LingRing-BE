package com.lingring.domain.friend.service;

import com.lingring.domain.friend.dao.FriendshipRepository;
import com.lingring.domain.friend.dao.dto.FriendItemProjection;
import com.lingring.domain.friend.domain.FriendRelation;
import com.lingring.domain.friend.domain.FriendRequestDirection;
import com.lingring.domain.friend.domain.Friendship;
import com.lingring.domain.friend.domain.FriendshipStatus;
import com.lingring.domain.friend.dto.request.FriendRequestCreateRequest;
import com.lingring.domain.friend.dto.request.FriendshipUpdateRequest;
import com.lingring.domain.friend.dto.response.FriendSearchResponse;
import com.lingring.domain.friend.dto.response.FriendshipResponse;
import com.lingring.domain.friend.dto.response.FriendsResponse;
import com.lingring.domain.friend.dto.response.ReceivedCountResponse;
import com.lingring.domain.friend.exception.AlreadyFriendsException;
import com.lingring.domain.friend.exception.DuplicateFriendRequestException;
import com.lingring.domain.friend.exception.FriendshipNotFoundException;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.global.common.pagination.PageSize;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Transactional
    public FriendshipResponse sendRequest(final Long userId, final FriendRequestCreateRequest request) {
        final Long targetUserId = request.targetUserId();
        final Optional<Friendship> existing = friendshipRepository.findBetween(userId, targetUserId);
        if (existing.isPresent()) {
            return resolveExistingOnRequest(userId, targetUserId, existing.get());
        }
        final Friendship saved = friendshipRepository.save(Friendship.request(userId, targetUserId));
        return FriendshipResponse.of(targetUserId, saved);
    }

    @Transactional
    public FriendshipResponse accept(
            final Long userId,
            final Long requesterUserId,
            final FriendshipUpdateRequest request
    ) {
        validateAcceptTransition(request.status());
        final Friendship friendship = friendshipRepository.findBetween(userId, requesterUserId)
                .orElseThrow(() -> new FriendshipNotFoundException(userId, requesterUserId));
        friendship.accept(userId);
        return FriendshipResponse.of(requesterUserId, friendship);
    }

    @Transactional
    public void remove(final Long userId, final Long otherUserId) {
        friendshipRepository.findBetween(userId, otherUserId)
                .ifPresent(friendshipRepository::delete);
    }

    @Transactional(readOnly = true)
    public FriendsResponse getFriends(
            final Long userId,
            final FriendshipStatus status,
            final FriendRequestDirection direction,
            final int page,
            final int size
    ) {
        final boolean includeSent = includesSent(direction);
        final boolean includeReceived = includesReceived(direction);
        final PageSize pageSize = PageSize.clamp(size);
        final Slice<FriendItemProjection> slice = friendshipRepository.findItemsByUserIdAndStatus(
                userId, status, includeSent, includeReceived, PageRequest.of(page, pageSize.value()));
        return FriendsResponse.from(slice);
    }

    @Transactional(readOnly = true)
    public Optional<FriendSearchResponse> search(final Long userId, final String nickname) {
        return userRepository.findSearchProfileByNickname(nickname)
                .map(profile -> FriendSearchResponse.of(profile, resolveRelation(userId, profile.getId())));
    }

    @Transactional(readOnly = true)
    public ReceivedCountResponse receivedRequestCount(final Long userId) {
        final long count = friendshipRepository.countByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING);
        return new ReceivedCountResponse(count);
    }

    // direction 미지정(null)이면 양방향을 포함한다.
    private boolean includesSent(final FriendRequestDirection direction) {
        return direction != FriendRequestDirection.RECEIVED;
    }

    private boolean includesReceived(final FriendRequestDirection direction) {
        return direction != FriendRequestDirection.SENT;
    }

    private FriendRelation resolveRelation(final Long userId, final Long targetUserId) {
        if (userId.equals(targetUserId)) {
            return FriendRelation.SELF;
        }
        final Optional<Friendship> existing = friendshipRepository.findBetween(userId, targetUserId);
        if (existing.isEmpty()) {
            return FriendRelation.NONE;
        }
        final Friendship friendship = existing.get();
        if (!friendship.isPending()) {
            return FriendRelation.FRIEND;
        }
        if (friendship.getRequesterId().equals(userId)) {
            return FriendRelation.REQUEST_SENT;
        }
        return FriendRelation.REQUEST_RECEIVED;
    }

    private FriendshipResponse resolveExistingOnRequest(
            final Long userId,
            final Long targetUserId,
            final Friendship existing
    ) {
        if (!existing.isPending()) {
            throw new AlreadyFriendsException(userId, targetUserId);
        }
        if (existing.getRequesterId().equals(userId)) {
            throw new DuplicateFriendRequestException(userId, targetUserId);
        }
        // 역방향 대기 요청 존재: 상대가 이미 나에게 보냈으므로 새 요청 대신 즉시 수락 처리한다.
        existing.accept(userId);
        return FriendshipResponse.of(targetUserId, existing);
    }

    private void validateAcceptTransition(final FriendshipStatus status) {
        if (status != FriendshipStatus.ACCEPTED) {
            throw new BadRequestException(
                    ErrorCode.NOT_SUPPORTED,
                    "친구 관계는 ACCEPTED로만 전이할 수 있습니다. 요청 상태: %s".formatted(status)
            );
        }
    }
}
