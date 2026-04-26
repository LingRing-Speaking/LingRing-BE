package com.lingring.domain.userblock.dto.response;

import com.lingring.domain.userblock.domain.UserBlock;
import java.util.List;
import org.springframework.data.domain.Slice;

public record UserBlockListResponse(
        List<UserBlockResponse> items,
        boolean hasNext
) {

    public static UserBlockListResponse from(final Slice<UserBlock> slice) {
        final List<UserBlockResponse> items = slice.getContent().stream()
                .map(UserBlockResponse::from)
                .toList();
        return new UserBlockListResponse(items, slice.hasNext());
    }
}
