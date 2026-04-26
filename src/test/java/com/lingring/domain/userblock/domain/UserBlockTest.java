package com.lingring.domain.userblock.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserBlockTest {

    @Test
    @DisplayName("create는 userId와 blockedUserId를 보유한 엔티티를 생성한다")
    void createWrapsValuesIntoEntity() {
        // given
        final Long userId = 1L;
        final Long blockedUserId = 2L;

        // when
        final UserBlock userBlock = UserBlock.create(userId, blockedUserId);

        // then
        assertThat(userBlock.getUserId()).isEqualTo(userId);
        assertThat(userBlock.getBlockedUserId()).isEqualTo(blockedUserId);
    }
}
