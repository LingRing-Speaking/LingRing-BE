package com.lingring.domain.userblock.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.userblock.domain.UserBlock;
import com.lingring.global.config.RepositoryTestHelper;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

class UserBlockRepositoryTest extends RepositoryTestHelper {

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Nested
    @DisplayName("existsByUserIdAndBlockedUserId")
    class ExistsByUserIdAndBlockedUserId {

        @Test
        @DisplayName("동일한 (userId, blockedUserId) 행이 있으면 true를 반환한다")
        void returnsTrueWhenExists() {
            // given
            userBlockRepository.save(UserBlock.create(1L, 2L));

            // when
            final boolean exists = userBlockRepository.existsByUserIdAndBlockedUserId(1L, 2L);

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("방향이 반대면 false를 반환한다 (단방향 차단)")
        void returnsFalseWhenReversed() {
            // given
            userBlockRepository.save(UserBlock.create(1L, 2L));

            // when
            final boolean exists = userBlockRepository.existsByUserIdAndBlockedUserId(2L, 1L);

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndBlockedUserId")
    class FindByUserIdAndBlockedUserId {

        @Test
        @DisplayName("두 id가 모두 일치하면 반환한다")
        void returnsWhenBothMatch() {
            // given
            final UserBlock saved = userBlockRepository.save(UserBlock.create(1L, 2L));

            // when
            final Optional<UserBlock> found =
                    userBlockRepository.findByUserIdAndBlockedUserId(1L, 2L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("일치하지 않으면 Optional.empty()를 반환한다")
        void returnsEmptyWhenNoMatch() {
            // given
            userBlockRepository.save(UserBlock.create(1L, 2L));

            // when
            final Optional<UserBlock> found =
                    userBlockRepository.findByUserIdAndBlockedUserId(1L, 3L);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByUserIdOrderByCreatedAtDesc")
    class FindAllByUserIdOrderByCreatedAtDesc {

        @Test
        @DisplayName("userId 필터링: 해당 사용자의 차단 항목만 반환한다")
        void returnsOnlyUsersBlocks() {
            // given
            userBlockRepository.save(UserBlock.create(1L, 10L));
            userBlockRepository.save(UserBlock.create(1L, 11L));
            userBlockRepository.save(UserBlock.create(2L, 20L));

            // when
            final Slice<UserBlock> slice = userBlockRepository
                    .findAllByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent()).hasSize(2);
            assertThat(slice.getContent()).allMatch(item -> item.getUserId().equals(1L));
            assertThat(slice.hasNext()).isFalse();
        }

        @Test
        @DisplayName("저장 건수보다 작은 size로 요청하면 hasNext가 true다")
        void hasNextTrueWhenMoreItemsRemain() {
            // given
            userBlockRepository.save(UserBlock.create(1L, 10L));
            userBlockRepository.save(UserBlock.create(1L, 11L));
            userBlockRepository.save(UserBlock.create(1L, 12L));

            // when
            final Slice<UserBlock> slice = userBlockRepository
                    .findAllByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 1));

            // then
            assertThat(slice.getContent()).hasSize(1);
            assertThat(slice.hasNext()).isTrue();
        }

        @Test
        @DisplayName("userId에 해당하는 항목이 없으면 빈 Slice 반환")
        void returnsEmptySliceWhenNone() {
            // when
            final Slice<UserBlock> slice = userBlockRepository
                    .findAllByUserIdOrderByCreatedAtDesc(9_999_999L, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent()).isEmpty();
            assertThat(slice.hasNext()).isFalse();
        }
    }
}
