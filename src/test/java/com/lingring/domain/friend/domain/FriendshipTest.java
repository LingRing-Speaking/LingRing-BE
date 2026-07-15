package com.lingring.domain.friend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.friend.exception.FriendshipAccessDeniedException;
import com.lingring.domain.friend.exception.SelfFriendshipException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FriendshipTest {

    @Nested
    @DisplayName("request: 친구 요청 생성")
    class Request {

        @Test
        @DisplayName("requester/addressee를 보유하고 PENDING 상태로 생성된다")
        void request_whenValid_createsPending() {
            // given
            final Long requesterId = 1L;
            final Long addresseeId = 2L;

            // when
            final Friendship friendship = Friendship.request(requesterId, addresseeId);

            // then
            assertThat(friendship.getRequesterId()).isEqualTo(requesterId);
            assertThat(friendship.getAddresseeId()).isEqualTo(addresseeId);
            assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);
            assertThat(friendship.isPending()).isTrue();
        }

        @Test
        @DisplayName("자기 자신에게 요청하면 SelfFriendshipException이 발생한다")
        void request_whenSelf_throws() {
            // given
            final Long userId = 1L;

            // when & then
            assertThatThrownBy(() -> Friendship.request(userId, userId))
                    .isInstanceOf(SelfFriendshipException.class);
        }
    }

    @Nested
    @DisplayName("accept: 친구 요청 수락")
    class Accept {

        @Test
        @DisplayName("요청을 받은 addressee가 수락하면 ACCEPTED로 전이된다")
        void accept_whenAddressee_transitionsToAccepted() {
            // given
            final Friendship friendship = Friendship.request(1L, 2L);

            // when
            friendship.accept(2L);

            // then
            assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
            assertThat(friendship.isPending()).isFalse();
        }

        @Test
        @DisplayName("addressee가 아닌 사용자(요청자)가 수락하면 FriendshipAccessDeniedException이 발생한다")
        void accept_whenNotAddressee_throws() {
            // given
            final Friendship friendship = Friendship.request(1L, 2L);

            // when & then
            assertThatThrownBy(() -> friendship.accept(1L))
                    .isInstanceOf(FriendshipAccessDeniedException.class);
            assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);
        }
    }
}
