package com.lingring.domain.friend.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.friend.dao.dto.FriendItemProjection;
import com.lingring.domain.friend.domain.Friendship;
import com.lingring.domain.friend.domain.FriendshipStatus;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.config.RepositoryTestHelper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

class FriendshipRepositoryTest extends RepositoryTestHelper {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("findBetween")
    class FindBetween {

        @Test
        @DisplayName("정방향(requester=a, addressee=b)으로 저장돼 있으면 반환한다")
        void returnsWhenForward() {
            // given
            final Friendship saved = friendshipRepository.save(Friendship.request(1L, 2L));

            // when
            final Optional<Friendship> found = friendshipRepository.findBetween(1L, 2L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("역방향(requester=b, addressee=a)이어도 방향과 무관하게 반환한다")
        void returnsWhenReversed() {
            // given
            final Friendship saved = friendshipRepository.save(Friendship.request(1L, 2L));

            // when
            final Optional<Friendship> found = friendshipRepository.findBetween(2L, 1L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("두 사용자 사이에 관계가 없으면 Optional.empty()를 반환한다")
        void returnsEmptyWhenNone() {
            // given
            friendshipRepository.save(Friendship.request(1L, 2L));

            // when
            final Optional<Friendship> found = friendshipRepository.findBetween(1L, 3L);

            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findItemsByUserIdAndStatus")
    class FindItemsByUserIdAndStatus {

        @Test
        @DisplayName("ACCEPTED 조회: 내가 요청한/받은 친구 모두 상대 프로필과 함께 반환한다")
        void returnsAcceptedFriendsWithCounterpartProfile() {
            // given
            final User me = saveUser("미나", null);
            final User sent = saveUser("내가보낸친구", "https://cdn.example.com/sent.png");
            final User received = saveUser("나에게보낸친구", "https://cdn.example.com/received.png");
            saveFriendship(me.getId(), sent.getId(), FriendshipStatus.ACCEPTED);
            saveFriendship(received.getId(), me.getId(), FriendshipStatus.ACCEPTED);

            // when
            final Slice<FriendItemProjection> slice = friendshipRepository
                    .findItemsByUserIdAndStatus(me.getId(), FriendshipStatus.ACCEPTED, true, true, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent()).hasSize(2);
            assertThat(slice.getContent()).extracting(FriendItemProjection::getUserId)
                    .containsExactlyInAnyOrder(sent.getId(), received.getId());
            assertThat(slice.getContent()).extracting(FriendItemProjection::getNickname)
                    .containsExactlyInAnyOrder("내가보낸친구", "나에게보낸친구");
        }

        @Test
        @DisplayName("PENDING 조회: direction으로 보낸(SENT)/받은(RECEIVED) 요청을 구분한다")
        void distinguishesDirectionForPending() {
            // given
            final User me = saveUser("미나", null);
            final User iSentTo = saveUser("내가보낸대상", null);
            final User sentToMe = saveUser("나에게보낸사람", null);
            saveFriendship(me.getId(), iSentTo.getId(), FriendshipStatus.PENDING);
            saveFriendship(sentToMe.getId(), me.getId(), FriendshipStatus.PENDING);

            // when
            final Slice<FriendItemProjection> slice = friendshipRepository
                    .findItemsByUserIdAndStatus(me.getId(), FriendshipStatus.PENDING, true, true, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent())
                    .filteredOn(item -> item.getUserId().equals(iSentTo.getId()))
                    .singleElement()
                    .extracting(FriendItemProjection::getDirection)
                    .isEqualTo("SENT");
            assertThat(slice.getContent())
                    .filteredOn(item -> item.getUserId().equals(sentToMe.getId()))
                    .singleElement()
                    .extracting(FriendItemProjection::getDirection)
                    .isEqualTo("RECEIVED");
        }

        @Test
        @DisplayName("status 필터: 다른 상태의 관계는 반환하지 않는다")
        void filtersByStatus() {
            // given
            final User me = saveUser("미나", null);
            final User accepted = saveUser("친구", null);
            final User pending = saveUser("대기", null);
            saveFriendship(me.getId(), accepted.getId(), FriendshipStatus.ACCEPTED);
            saveFriendship(me.getId(), pending.getId(), FriendshipStatus.PENDING);

            // when
            final Slice<FriendItemProjection> slice = friendshipRepository
                    .findItemsByUserIdAndStatus(me.getId(), FriendshipStatus.ACCEPTED, true, true, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent()).hasSize(1);
            assertThat(slice.getContent().get(0).getUserId()).isEqualTo(accepted.getId());
        }

        @Test
        @DisplayName("저장 건수보다 작은 size로 요청하면 hasNext가 true다")
        void hasNextTrueWhenMoreItemsRemain() {
            // given
            final User me = saveUser("미나", null);
            saveFriendship(me.getId(), saveUser("친구A", null).getId(), FriendshipStatus.ACCEPTED);
            saveFriendship(me.getId(), saveUser("친구B", null).getId(), FriendshipStatus.ACCEPTED);

            // when
            final Slice<FriendItemProjection> slice = friendshipRepository
                    .findItemsByUserIdAndStatus(me.getId(), FriendshipStatus.ACCEPTED, true, true, PageRequest.of(0, 1));

            // then
            assertThat(slice.getContent()).hasSize(1);
            assertThat(slice.hasNext()).isTrue();
        }

        @Test
        @DisplayName("direction 필터: includeReceived만 true면 받은 요청만 반환한다")
        void directionFilterReturnsOnlyReceived() {
            // given
            final User me = saveUser("미나", null);
            final User iSentTo = saveUser("내가보낸대상", null);
            final User sentToMe = saveUser("나에게보낸사람", null);
            saveFriendship(me.getId(), iSentTo.getId(), FriendshipStatus.PENDING);
            saveFriendship(sentToMe.getId(), me.getId(), FriendshipStatus.PENDING);

            // when (받은 것만)
            final Slice<FriendItemProjection> slice = friendshipRepository.findItemsByUserIdAndStatus(
                    me.getId(), FriendshipStatus.PENDING, false, true, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent()).hasSize(1);
            assertThat(slice.getContent().get(0).getUserId()).isEqualTo(sentToMe.getId());
            assertThat(slice.getContent().get(0).getDirection()).isEqualTo("RECEIVED");
        }

        @Test
        @DisplayName("상대가 탈퇴(삭제)되어도 userId는 유지되고 nickname/profileImage만 null (LEFT JOIN)")
        void keepsUserIdWhenCounterpartWithdrawn() {
            // given
            final User me = saveUser("미나", null);
            final User withdrawn = saveUser("탈퇴자", "https://cdn.example.com/gone.png");
            final Long counterpartId = withdrawn.getId();
            saveFriendship(me.getId(), counterpartId, FriendshipStatus.ACCEPTED);
            userRepository.deleteById(counterpartId);

            // when
            final Slice<FriendItemProjection> slice = friendshipRepository.findItemsByUserIdAndStatus(
                    me.getId(), FriendshipStatus.ACCEPTED, true, true, PageRequest.of(0, 10));

            // then
            assertThat(slice.getContent()).hasSize(1);
            final FriendItemProjection item = slice.getContent().get(0);
            assertThat(item.getUserId()).isEqualTo(counterpartId);
            assertThat(item.getNickname()).isNull();
            assertThat(item.getProfileImage()).isNull();
        }
    }

    @Nested
    @DisplayName("countByAddresseeIdAndStatus")
    class CountByAddresseeIdAndStatus {

        @Test
        @DisplayName("나에게 온 PENDING 요청 수만 센다 (보낸 요청·다른 상태 제외)")
        void countsOnlyReceivedPending() {
            // given
            final Long me = 1L;
            friendshipRepository.save(Friendship.request(2L, me));   // 받은 PENDING
            friendshipRepository.save(Friendship.request(3L, me));   // 받은 PENDING
            friendshipRepository.save(Friendship.request(me, 4L));   // 내가 보낸 PENDING(제외)
            final Friendship accepted = Friendship.request(5L, me);  // 이미 친구(제외)
            accepted.accept(me);
            friendshipRepository.save(accepted);

            // when
            final long count = friendshipRepository.countByAddresseeIdAndStatus(me, FriendshipStatus.PENDING);

            // then
            assertThat(count).isEqualTo(2);
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

    private void saveFriendship(final Long requesterId, final Long addresseeId, final FriendshipStatus status) {
        final Friendship friendship = Friendship.request(requesterId, addresseeId);
        if (status == FriendshipStatus.ACCEPTED) {
            friendship.accept(addresseeId);
        }
        friendshipRepository.save(friendship);
    }
}
