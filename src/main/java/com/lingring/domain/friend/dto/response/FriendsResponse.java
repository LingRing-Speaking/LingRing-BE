package com.lingring.domain.friend.dto.response;

import com.lingring.domain.friend.dao.dto.FriendItemProjection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Slice;

public record FriendsResponse(
        List<FriendItemResponse> items,
        boolean hasNext
) {

    public static FriendsResponse of(final Slice<FriendItemProjection> slice, final Set<Long> onlineUserIds) {
        final List<FriendItemResponse> items = slice.getContent().stream()
                .map(projection -> FriendItemResponse.of(projection, onlineUserIds.contains(projection.getUserId())))
                .toList();
        return new FriendsResponse(items, slice.hasNext());
    }
}
