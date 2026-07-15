package com.lingring.domain.friend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.lingring.domain.friend.dao.FriendshipRepository;
import com.lingring.domain.friend.domain.FriendRelation;
import com.lingring.domain.friend.domain.Friendship;
import com.lingring.domain.friend.domain.FriendRequestDirection;
import com.lingring.domain.friend.domain.FriendshipStatus;
import com.lingring.domain.friend.dto.request.FriendRequestCreateRequest;
import com.lingring.domain.friend.dto.request.FriendshipUpdateRequest;
import com.lingring.domain.friend.dto.response.FriendItemResponse;
import com.lingring.domain.friend.dto.response.FriendSearchResponse;
import com.lingring.domain.friend.dto.response.FriendshipResponse;
import com.lingring.domain.friend.dto.response.FriendsResponse;
import com.lingring.domain.friend.exception.AlreadyFriendsException;
import com.lingring.domain.friend.exception.DuplicateFriendRequestException;
import com.lingring.domain.friend.exception.FriendshipAccessDeniedException;
import com.lingring.domain.friend.exception.FriendshipNotFoundException;
import com.lingring.domain.friend.exception.SelfFriendshipException;
import com.lingring.domain.presence.dao.PresenceRepository;
import com.lingring.domain.user.dao.UserRepository;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.BadRequestException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FriendServiceTest extends ServiceIntegrationHelper {

    @Autowired
    private FriendService friendService;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PresenceRepository presenceRepository;

    @Nested
    @DisplayName("sendRequest: 친구 요청 보내기")
    class SendRequest {

        @Test
        @DisplayName("기존 관계가 없으면 PENDING 요청이 생성된다")
        void sendRequest_whenNoExisting_createsPending() {
            // given
            final Long userId = 1L;
            final FriendRequestCreateRequest request = new FriendRequestCreateRequest(2L);

            // when
            final FriendshipResponse response = friendService.sendRequest(userId, request);

            // then
            assertThat(response.userId()).isEqualTo(2L);
            assertThat(response.status()).isEqualTo(FriendshipStatus.PENDING);
            assertThat(friendshipRepository.findBetween(1L, 2L)).isPresent();
        }

        @Test
        @DisplayName("상대가 이미 나에게 보낸 대기 요청이 있으면 즉시 ACCEPTED로 성립된다")
        void sendRequest_whenReversePending_autoAccepts() {
            // given
            final Long me = 1L;
            final Long other = 2L;
            friendshipRepository.save(Friendship.request(other, me));

            // when
            final FriendshipResponse response =
                    friendService.sendRequest(me, new FriendRequestCreateRequest(other));

            // then
            assertThat(response.status()).isEqualTo(FriendshipStatus.ACCEPTED);
            assertThat(friendshipRepository.findBetween(me, other))
                    .get()
                    .extracting(Friendship::getStatus)
                    .isEqualTo(FriendshipStatus.ACCEPTED);
        }

        @Test
        @DisplayName("같은 방향으로 이미 보낸 대기 요청이 있으면 DuplicateFriendRequestException")
        void sendRequest_whenDuplicate_throws() {
            // given
            final Long userId = 1L;
            friendshipRepository.save(Friendship.request(userId, 2L));

            // when & then
            assertThatThrownBy(() -> friendService.sendRequest(userId, new FriendRequestCreateRequest(2L)))
                    .isInstanceOf(DuplicateFriendRequestException.class);
        }

        @Test
        @DisplayName("이미 친구인 대상에게 요청하면 AlreadyFriendsException")
        void sendRequest_whenAlreadyFriends_throws() {
            // given
            final Long userId = 1L;
            final Friendship accepted = Friendship.request(userId, 2L);
            accepted.accept(2L);
            friendshipRepository.save(accepted);

            // when & then
            assertThatThrownBy(() -> friendService.sendRequest(userId, new FriendRequestCreateRequest(2L)))
                    .isInstanceOf(AlreadyFriendsException.class);
        }

        @Test
        @DisplayName("자기 자신에게 요청하면 SelfFriendshipException")
        void sendRequest_whenSelf_throws() {
            // given
            final Long userId = 1L;

            // when & then
            assertThatThrownBy(() -> friendService.sendRequest(userId, new FriendRequestCreateRequest(userId)))
                    .isInstanceOf(SelfFriendshipException.class);
        }
    }

    @Nested
    @DisplayName("accept: 친구 요청 수락")
    class Accept {

        @Test
        @DisplayName("요청을 받은 addressee가 수락하면 ACCEPTED로 전이된다")
        void accept_whenAddressee_accepts() {
            // given
            final Long requester = 1L;
            final Long addressee = 2L;
            friendshipRepository.save(Friendship.request(requester, addressee));

            // when
            final FriendshipResponse response =
                    friendService.accept(addressee, requester, new FriendshipUpdateRequest(FriendshipStatus.ACCEPTED));

            // then
            assertThat(response.status()).isEqualTo(FriendshipStatus.ACCEPTED);
            assertThat(friendshipRepository.findBetween(requester, addressee))
                    .get()
                    .extracting(Friendship::getStatus)
                    .isEqualTo(FriendshipStatus.ACCEPTED);
        }

        @Test
        @DisplayName("요청을 보낸 requester가 자기 요청을 수락하려 하면 FriendshipAccessDeniedException")
        void accept_whenRequester_throws() {
            // given
            final Long requester = 1L;
            final Long addressee = 2L;
            friendshipRepository.save(Friendship.request(requester, addressee));

            // when & then
            assertThatThrownBy(() -> friendService.accept(
                    requester, addressee, new FriendshipUpdateRequest(FriendshipStatus.ACCEPTED)))
                    .isInstanceOf(FriendshipAccessDeniedException.class);
        }

        @Test
        @DisplayName("수락할 요청이 없으면 FriendshipNotFoundException")
        void accept_whenNoRequest_throws() {
            // when & then
            assertThatThrownBy(() -> friendService.accept(
                    1L, 2L, new FriendshipUpdateRequest(FriendshipStatus.ACCEPTED)))
                    .isInstanceOf(FriendshipNotFoundException.class);
        }

        @Test
        @DisplayName("body status가 ACCEPTED가 아니면 BadRequestException(NOT_SUPPORTED)")
        void accept_whenStatusNotAccepted_throws() {
            // given
            friendshipRepository.save(Friendship.request(1L, 2L));

            // when & then
            assertThatThrownBy(() -> friendService.accept(
                    2L, 1L, new FriendshipUpdateRequest(FriendshipStatus.PENDING)))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_SUPPORTED);
        }
    }

    @Nested
    @DisplayName("remove: 친구 관계 제거")
    class Remove {

        @Test
        @DisplayName("대기 요청을 제거하면 row가 삭제된다")
        void remove_whenPending_deletes() {
            // given
            final Friendship saved = friendshipRepository.save(Friendship.request(1L, 2L));

            // when
            friendService.remove(1L, 2L);

            // then
            assertThat(friendshipRepository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("성립된 친구를 제거하면 방향과 무관하게 삭제된다")
        void remove_whenAccepted_deletes() {
            // given
            final Friendship accepted = Friendship.request(1L, 2L);
            accepted.accept(2L);
            final Friendship saved = friendshipRepository.save(accepted);

            // when (반대 방향 인자로 호출)
            friendService.remove(2L, 1L);

            // then
            assertThat(friendshipRepository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("관계가 없어도 예외 없이 완료된다 (멱등)")
        void remove_whenNone_doesNotThrow() {
            // when & then
            friendService.remove(1L, 9_999_999L);
        }
    }

    @Nested
    @DisplayName("getFriends: 목록 조회")
    class GetFriends {

        @Test
        @DisplayName("ACCEPTED 조회 시 상대 프로필이 채워진다")
        void getFriends_accepted_includesProfile() {
            // given
            final User me = saveUser("미나", null);
            final User friend = saveUser("친구", "https://cdn.example.com/f.png");
            final Friendship accepted = Friendship.request(me.getId(), friend.getId());
            accepted.accept(friend.getId());
            friendshipRepository.save(accepted);

            // when
            final FriendsResponse response =
                    friendService.getFriends(me.getId(), FriendshipStatus.ACCEPTED, null, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            final FriendItemResponse item = response.items().get(0);
            assertThat(item.userId()).isEqualTo(friend.getId());
            assertThat(item.nickname()).isEqualTo("친구");
            assertThat(item.profileImage()).isEqualTo("https://cdn.example.com/f.png");
            assertThat(item.status()).isEqualTo(FriendshipStatus.ACCEPTED);
        }

        @Test
        @DisplayName("PENDING 조회 시 받은 요청은 direction=RECEIVED로 반환된다")
        void getFriends_pending_marksReceivedDirection() {
            // given
            final User me = saveUser("미나", null);
            final User requester = saveUser("보낸사람", null);
            friendshipRepository.save(Friendship.request(requester.getId(), me.getId()));

            // when
            final FriendsResponse response =
                    friendService.getFriends(me.getId(), FriendshipStatus.PENDING, null, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            final FriendItemResponse item = response.items().get(0);
            assertThat(item.userId()).isEqualTo(requester.getId());
            assertThat(item.direction()).isEqualTo(FriendRequestDirection.RECEIVED);
            assertThat(item.status()).isEqualTo(FriendshipStatus.PENDING);
        }

        @Test
        @DisplayName("온라인 친구는 online=true, 오프라인 친구는 online=false로 반환된다")
        void getFriends_marksOnlineStateOfFriends() {
            // given
            final User me = saveUser("미나", null);
            final User onlineFriend = saveUser("온라인친구", null);
            final User offlineFriend = saveUser("오프라인친구", null);
            saveAcceptedFriendship(me.getId(), onlineFriend.getId());
            saveAcceptedFriendship(me.getId(), offlineFriend.getId());
            presenceRepository.markOnline(onlineFriend.getId(), Duration.ofSeconds(10));

            // when
            final FriendsResponse response =
                    friendService.getFriends(me.getId(), FriendshipStatus.ACCEPTED, null, 0, 20);

            // then
            assertThat(response.items())
                    .extracting(FriendItemResponse::userId, FriendItemResponse::online)
                    .containsExactlyInAnyOrder(
                            tuple(onlineFriend.getId(), true),
                            tuple(offlineFriend.getId(), false)
                    );
        }

        @Test
        @DisplayName("direction=SENT면 내가 보낸 대기 요청만 반환한다")
        void getFriends_pending_directionSent_returnsOnlySent() {
            // given
            final Long me = 1L;
            friendshipRepository.save(Friendship.request(me, 2L));   // 내가 보냄
            friendshipRepository.save(Friendship.request(3L, me));   // 내가 받음

            // when
            final FriendsResponse response =
                    friendService.getFriends(me, FriendshipStatus.PENDING, FriendRequestDirection.SENT, 0, 20);

            // then
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).userId()).isEqualTo(2L);
            assertThat(response.items().get(0).direction()).isEqualTo(FriendRequestDirection.SENT);
        }
    }

    @Nested
    @DisplayName("search: 닉네임 검색")
    class Search {

        @Test
        @DisplayName("관계가 없는 사용자는 relation=NONE으로 반환한다")
        void search_whenNoRelation_returnsNone() {
            // given
            final User me = saveUser("미나", null);
            final User target = saveUser("지우", "https://cdn.example.com/j.png");

            // when
            final FriendSearchResponse response = friendService.search(me.getId(), "지우").orElseThrow();

            // then
            assertThat(response.userId()).isEqualTo(target.getId());
            assertThat(response.nickname()).isEqualTo("지우");
            assertThat(response.relation()).isEqualTo(FriendRelation.NONE);
        }

        @Test
        @DisplayName("내가 요청을 보낸 사용자는 relation=REQUEST_SENT")
        void search_whenRequestSent_returnsRequestSent() {
            // given
            final User me = saveUser("미나", null);
            final User target = saveUser("지우", null);
            friendshipRepository.save(Friendship.request(me.getId(), target.getId()));

            // when
            final FriendSearchResponse response = friendService.search(me.getId(), "지우").orElseThrow();

            // then
            assertThat(response.relation()).isEqualTo(FriendRelation.REQUEST_SENT);
        }

        @Test
        @DisplayName("나에게 요청을 보낸 사용자는 relation=REQUEST_RECEIVED")
        void search_whenRequestReceived_returnsRequestReceived() {
            // given
            final User me = saveUser("미나", null);
            final User target = saveUser("지우", null);
            friendshipRepository.save(Friendship.request(target.getId(), me.getId()));

            // when
            final FriendSearchResponse response = friendService.search(me.getId(), "지우").orElseThrow();

            // then
            assertThat(response.relation()).isEqualTo(FriendRelation.REQUEST_RECEIVED);
        }

        @Test
        @DisplayName("이미 친구인 사용자는 relation=FRIEND")
        void search_whenFriend_returnsFriend() {
            // given
            final User me = saveUser("미나", null);
            final User target = saveUser("지우", null);
            final Friendship accepted = Friendship.request(me.getId(), target.getId());
            accepted.accept(target.getId());
            friendshipRepository.save(accepted);

            // when
            final FriendSearchResponse response = friendService.search(me.getId(), "지우").orElseThrow();

            // then
            assertThat(response.relation()).isEqualTo(FriendRelation.FRIEND);
        }

        @Test
        @DisplayName("자기 자신을 검색하면 relation=SELF")
        void search_whenSelf_returnsSelf() {
            // given
            final User me = saveUser("미나", null);

            // when
            final FriendSearchResponse response = friendService.search(me.getId(), "미나").orElseThrow();

            // then
            assertThat(response.relation()).isEqualTo(FriendRelation.SELF);
        }

        @Test
        @DisplayName("일치하는 닉네임이 없으면 Optional.empty()")
        void search_whenNotFound_returnsEmpty() {
            // given
            saveUser("미나", null);

            // when & then
            assertThat(friendService.search(1L, "없는닉네임")).isEmpty();
        }
    }

    @Nested
    @DisplayName("receivedRequestCount: 받은 요청 개수")
    class ReceivedRequestCount {

        @Test
        @DisplayName("나에게 온 PENDING 요청만 센다 (보낸 요청·친구 제외)")
        void receivedRequestCount_countsOnlyReceivedPending() {
            // given
            final Long me = 1L;
            friendshipRepository.save(Friendship.request(2L, me));   // 받은 요청
            friendshipRepository.save(Friendship.request(3L, me));   // 받은 요청
            friendshipRepository.save(Friendship.request(me, 4L));   // 내가 보낸 요청(제외)
            final Friendship accepted = Friendship.request(5L, me);  // 이미 친구(제외)
            accepted.accept(me);
            friendshipRepository.save(accepted);

            // when
            final long count = friendService.receivedRequestCount(me).count();

            // then
            assertThat(count).isEqualTo(2);
        }
    }

    private void saveAcceptedFriendship(final Long requesterId, final Long addresseeId) {
        final Friendship friendship = Friendship.request(requesterId, addresseeId);
        friendship.accept(addresseeId);
        friendshipRepository.save(friendship);
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
