package com.lingring.domain.userblock.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.userblock.dao.dto.UserBlockItemProjection;
import com.lingring.domain.userblock.domain.UserBlock;
import com.lingring.global.config.RepositoryTestHelper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

class UserBlockRepositoryTest extends RepositoryTestHelper {

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private UserRepository userRepository;

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
    @DisplayName("findItemsByUserId")
    class FindItemsByUserId {

        @Test
        @DisplayName("userId 필터링: 해당 사용자의 차단 항목만 반환하고 차단된 사용자의 닉네임/프로필 이미지를 함께 반환한다")
        void returnsOnlyUsersBlocksWithBlockedUserProfile() {
            // given
            final Long me = 1L;
            final User blocked1 = saveUser("타깃1", "https://cdn.example.com/p1.png");
            final User blocked2 = saveUser("타깃2", null);
            final User otherUserBlocked = saveUser("타깃3", null);
            userBlockRepository.save(UserBlock.create(me, blocked1.getId()));
            userBlockRepository.save(UserBlock.create(me, blocked2.getId()));
            userBlockRepository.save(UserBlock.create(2L, otherUserBlocked.getId()));

            // when
            final Slice<UserBlockItemProjection> slice = userBlockRepository
                    .findItemsByUserId(me, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent()).hasSize(2);
            assertThat(slice.getContent()).extracting(UserBlockItemProjection::getBlockedUserId)
                    .containsExactlyInAnyOrder(blocked1.getId(), blocked2.getId());
            assertThat(slice.getContent()).extracting(UserBlockItemProjection::getNickname)
                    .containsExactlyInAnyOrder("타깃1", "타깃2");
            assertThat(slice.hasNext()).isFalse();
        }

        @Test
        @DisplayName("created_at DESC 정렬: 최근 차단이 먼저 온다")
        void returnsOrderedByCreatedAtDesc() {
            // given
            final Long me = 1L;
            final User first = saveUser("첫번째", null);
            final User second = saveUser("두번째", null);
            userBlockRepository.save(UserBlock.create(me, first.getId()));
            userBlockRepository.save(UserBlock.create(me, second.getId()));

            // when
            final Slice<UserBlockItemProjection> slice = userBlockRepository
                    .findItemsByUserId(me, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent()).extracting(UserBlockItemProjection::getBlockedUserId)
                    .containsExactly(second.getId(), first.getId());
        }

        @Test
        @DisplayName("차단된 사용자가 탈퇴(삭제)되었으면 nickname/profileImage가 null로 반환된다 (LEFT JOIN)")
        void returnsNullProfileWhenBlockedUserMissing() {
            // given
            final Long me = 1L;
            final User withdrawn = saveUser("탈퇴자", "https://cdn.example.com/gone.png");
            final Long blockedUserId = withdrawn.getId();
            userBlockRepository.save(UserBlock.create(me, blockedUserId));
            userRepository.deleteById(blockedUserId);

            // when
            final Slice<UserBlockItemProjection> slice = userBlockRepository
                    .findItemsByUserId(me, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent()).hasSize(1);
            final UserBlockItemProjection item = slice.getContent().get(0);
            assertThat(item.getBlockedUserId()).isEqualTo(blockedUserId);
            assertThat(item.getNickname()).isNull();
            assertThat(item.getProfileImage()).isNull();
        }

        @Test
        @DisplayName("저장 건수보다 작은 size로 요청하면 hasNext가 true다")
        void hasNextTrueWhenMoreItemsRemain() {
            // given
            final Long me = 1L;
            userBlockRepository.save(UserBlock.create(me, saveUser("유저A", null).getId()));
            userBlockRepository.save(UserBlock.create(me, saveUser("유저B", null).getId()));
            userBlockRepository.save(UserBlock.create(me, saveUser("유저C", null).getId()));

            // when
            final Slice<UserBlockItemProjection> slice = userBlockRepository
                    .findItemsByUserId(me, PageRequest.of(0, 1));

            // then
            assertThat(slice.getContent()).hasSize(1);
            assertThat(slice.hasNext()).isTrue();
        }

        @Test
        @DisplayName("userId에 해당하는 항목이 없으면 빈 Slice 반환")
        void returnsEmptySliceWhenNone() {
            // when
            final Slice<UserBlockItemProjection> slice = userBlockRepository
                    .findItemsByUserId(9_999_999L, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent()).isEmpty();
            assertThat(slice.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("findBlockedUserIdsByUserId")
    class FindBlockedUserIdsByUserId {

        @Test
        @DisplayName("userId가 차단한 모든 blockedUserId를 반환한다")
        void returnsAllBlockedIds() {
            // given
            userBlockRepository.save(UserBlock.create(1L, 10L));
            userBlockRepository.save(UserBlock.create(1L, 11L));
            userBlockRepository.save(UserBlock.create(2L, 20L));

            // when
            final List<Long> blockedIds = userBlockRepository.findBlockedUserIdsByUserId(1L);

            // then
            assertThat(blockedIds).containsExactlyInAnyOrder(10L, 11L);
        }

        @Test
        @DisplayName("차단 기록이 없으면 빈 리스트를 반환한다")
        void returnsEmptyWhenNone() {
            // when
            final List<Long> blockedIds = userBlockRepository.findBlockedUserIdsByUserId(9_999_999L);

            // then
            assertThat(blockedIds).isEmpty();
        }
    }

    @Nested
    @DisplayName("findUserIdsByBlockedUserId")
    class FindUserIdsByBlockedUserId {

        @Test
        @DisplayName("blockedUserId를 차단한 모든 userId를 반환한다")
        void returnsAllBlockerIds() {
            // given
            userBlockRepository.save(UserBlock.create(1L, 10L));
            userBlockRepository.save(UserBlock.create(2L, 10L));
            userBlockRepository.save(UserBlock.create(3L, 11L));

            // when
            final List<Long> blockerIds = userBlockRepository.findUserIdsByBlockedUserId(10L);

            // then
            assertThat(blockerIds).containsExactlyInAnyOrder(1L, 2L);
        }

        @Test
        @DisplayName("아무도 해당 사용자를 차단하지 않았다면 빈 리스트를 반환한다")
        void returnsEmptyWhenNone() {
            // when
            final List<Long> blockerIds = userBlockRepository.findUserIdsByBlockedUserId(9_999_999L);

            // then
            assertThat(blockerIds).isEmpty();
        }
    }

    private User saveUser(final String name, final String profileImageUrl) {
        return userRepository.save(
                User.createFromOAuth(
                        Provider.KAKAO,
                        "sub-" + name + "-" + UUID.randomUUID(),
                        new Name(name),
                        profileImageUrl
                )
        );
    }
}
