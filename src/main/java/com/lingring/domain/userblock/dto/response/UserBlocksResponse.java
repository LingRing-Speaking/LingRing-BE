package com.lingring.domain.userblock.dto.response;

import com.lingring.domain.userblock.dao.dto.UserBlockItemProjection;
import java.util.List;
import org.springframework.data.domain.Slice;

public record UserBlocksResponse(
        List<UserBlockItemResponse> items,
        boolean hasNext
) {

    public static UserBlocksResponse from(final Slice<UserBlockItemProjection> slice) {
        final List<UserBlockItemResponse> items = slice.getContent().stream()
                .map(UserBlockItemResponse::from)
                .toList();
        return new UserBlocksResponse(items, slice.hasNext());
    }
}
