package com.lingring.domain.friend.dto.response;

import com.lingring.domain.friend.dao.dto.FriendItemProjection;
import java.util.List;
import org.springframework.data.domain.Slice;

public record FriendsResponse(
        List<FriendItemResponse> items,
        boolean hasNext
) {

    public static FriendsResponse from(final Slice<FriendItemProjection> slice) {
        final List<FriendItemResponse> items = slice.getContent().stream()
                .map(FriendItemResponse::from)
                .toList();
        return new FriendsResponse(items, slice.hasNext());
    }
}
