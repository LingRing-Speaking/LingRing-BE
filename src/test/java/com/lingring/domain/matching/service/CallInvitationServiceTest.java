package com.lingring.domain.matching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.friend.dao.FriendshipRepository;
import com.lingring.domain.friend.domain.Friendship;
import com.lingring.domain.friend.exception.FriendshipNotFoundException;
import com.lingring.domain.matching.dao.CallInvitationRepository;
import com.lingring.domain.matching.domain.CallInvitation;
import com.lingring.domain.matching.domain.CallInvitationPollStatus;
import com.lingring.domain.matching.dto.response.CallInvitationAcceptResponse;
import com.lingring.domain.matching.dto.response.CallInvitationStatusResponse;
import com.lingring.domain.matching.exception.CallInvitationAlreadySentException;
import com.lingring.domain.matching.exception.CallInvitationNotFoundException;
import com.lingring.domain.matching.exception.InviteeBusyException;
import com.lingring.domain.matching.exception.InviteeOfflineException;
import com.lingring.domain.matching.exception.SelfCallInvitationException;
import com.lingring.domain.presence.dao.PresenceRepository;
import com.lingring.global.config.ServiceIntegrationHelper;
import com.lingring.global.util.FixedDateTimeProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(MatchingServiceTestConfig.class)
class CallInvitationServiceTest extends ServiceIntegrationHelper {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 12, 12, 0, 0);
    private static final Long INVITER = 1L;
    private static final Long INVITEE = 2L;

    @Autowired
    private FixedDateTimeProvider dateTimeProvider;

    @Autowired
    private CallInvitationService callInvitationService;

    @Autowired
    private CallInvitationRepository callInvitationRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private PresenceRepository presenceRepository;

    @Autowired
    private CallRepository callRepository;

    @BeforeEach
    void stubDefaultTime() {
        dateTimeProvider.setFixedTime(FIXED_NOW);
    }

    private void befriend(final Long userId, final Long otherUserId) {
        final Friendship friendship = Friendship.request(userId, otherUserId);
        friendship.accept(otherUserId);
        friendshipRepository.save(friendship);
    }

    private void markOnline(final Long userId) {
        presenceRepository.markOnline(userId, Duration.ofSeconds(10));
    }

    private void seedInvitableFriend(final Long inviterId, final Long inviteeId) {
        befriend(inviterId, inviteeId);
        markOnline(inviteeId);
    }

    @Nested
    @DisplayName("invite: 통화 초대 생성")
    class Invite {

        @Test
        @DisplayName("친구이면서 온라인인 수신자에게 초대를 생성한다")
        void invite_whenFriendOnline_createsInvitation() {
            // given
            seedInvitableFriend(INVITER, INVITEE);

            // when
            callInvitationService.invite(INVITER, INVITEE);

            // then
            final Optional<CallInvitation> invitation = callInvitationRepository.findByInvitee(INVITEE);
            assertThat(invitation).isPresent();
            assertThat(invitation.get().inviterId()).isEqualTo(INVITER);
            assertThat(invitation.get().deadline()).isEqualTo(FIXED_NOW.plusSeconds(30));
        }

        @Test
        @DisplayName("자기 자신에게는 초대를 보낼 수 없다")
        void invite_whenSelf_throws() {
            assertThatThrownBy(() -> callInvitationService.invite(INVITER, INVITER))
                    .isInstanceOf(SelfCallInvitationException.class);
        }

        @Test
        @DisplayName("친구 관계가 없으면 초대할 수 없다")
        void invite_whenNotFriends_throws() {
            // given
            markOnline(INVITEE);

            // when & then
            assertThatThrownBy(() -> callInvitationService.invite(INVITER, INVITEE))
                    .isInstanceOf(FriendshipNotFoundException.class);
        }

        @Test
        @DisplayName("친구 요청이 PENDING 상태면 초대할 수 없다")
        void invite_whenFriendshipPending_throws() {
            // given
            friendshipRepository.save(Friendship.request(INVITER, INVITEE));
            markOnline(INVITEE);

            // when & then
            assertThatThrownBy(() -> callInvitationService.invite(INVITER, INVITEE))
                    .isInstanceOf(FriendshipNotFoundException.class);
        }

        @Test
        @DisplayName("수신자가 오프라인이면 즉시 실패한다")
        void invite_whenInviteeOffline_throws() {
            // given
            befriend(INVITER, INVITEE);

            // when & then
            assertThatThrownBy(() -> callInvitationService.invite(INVITER, INVITEE))
                    .isInstanceOf(InviteeOfflineException.class);
        }

        @Test
        @DisplayName("이미 진행 중인 발신 초대가 있으면 실패한다")
        void invite_whenAlreadySent_throws() {
            // given
            seedInvitableFriend(INVITER, INVITEE);
            seedInvitableFriend(INVITER, 3L);
            callInvitationService.invite(INVITER, INVITEE);

            // when & then
            assertThatThrownBy(() -> callInvitationService.invite(INVITER, 3L))
                    .isInstanceOf(CallInvitationAlreadySentException.class);
        }

        @Test
        @DisplayName("수신자가 다른 초대를 받는 중이면 실패한다")
        void invite_whenInviteeBusy_throws() {
            // given
            seedInvitableFriend(INVITER, INVITEE);
            seedInvitableFriend(3L, INVITEE);
            callInvitationService.invite(3L, INVITEE);

            // when & then
            assertThatThrownBy(() -> callInvitationService.invite(INVITER, INVITEE))
                    .isInstanceOf(InviteeBusyException.class);
        }
    }

    @Nested
    @DisplayName("getOutgoingStatus: 발신 상태 폴링")
    class GetOutgoingStatus {

        @Test
        @DisplayName("진행 중인 초대가 있으면 RINGING을 반환한다")
        void status_whenInvitationPending_returnsRinging() {
            // given
            seedInvitableFriend(INVITER, INVITEE);
            callInvitationService.invite(INVITER, INVITEE);

            // when
            final CallInvitationStatusResponse response = callInvitationService.getOutgoingStatus(INVITER);

            // then
            assertThat(response.status()).isEqualTo(CallInvitationPollStatus.RINGING);
        }

        @Test
        @DisplayName("초대도 결과도 없으면 NONE을 반환한다")
        void status_whenNothing_returnsNone() {
            // when
            final CallInvitationStatusResponse response = callInvitationService.getOutgoingStatus(INVITER);

            // then
            assertThat(response.status()).isEqualTo(CallInvitationPollStatus.NONE);
        }

        @Test
        @DisplayName("수락되면 roomId·callId가 포함된 ACCEPTED를 반환한다")
        void status_whenAccepted_returnsAcceptedWithCallId() {
            // given
            seedInvitableFriend(INVITER, INVITEE);
            callInvitationService.invite(INVITER, INVITEE);
            final CallInvitationAcceptResponse accepted = callInvitationService.accept(INVITEE);

            // when
            final CallInvitationStatusResponse response = callInvitationService.getOutgoingStatus(INVITER);

            // then
            assertThat(response.status()).isEqualTo(CallInvitationPollStatus.ACCEPTED);
            assertThat(response.roomId()).isEqualTo(accepted.roomId());
            assertThat(response.callId()).isEqualTo(accepted.callId());
        }

        @Test
        @DisplayName("거절되면 DECLINED를 반환한다")
        void status_whenDeclined_returnsDeclined() {
            // given
            seedInvitableFriend(INVITER, INVITEE);
            callInvitationService.invite(INVITER, INVITEE);
            callInvitationService.decline(INVITEE);

            // when
            final CallInvitationStatusResponse response = callInvitationService.getOutgoingStatus(INVITER);

            // then
            assertThat(response.status()).isEqualTo(CallInvitationPollStatus.DECLINED);
        }
    }

    @Nested
    @DisplayName("cancel: 발신 취소")
    class Cancel {

        @Test
        @DisplayName("초대를 제거해 수신자에게 더 이상 노출되지 않는다")
        void cancel_removesInvitation() {
            // given
            seedInvitableFriend(INVITER, INVITEE);
            callInvitationService.invite(INVITER, INVITEE);

            // when
            callInvitationService.cancel(INVITER);

            // then
            assertThat(callInvitationService.findIncoming(INVITEE)).isEmpty();
            assertThat(callInvitationService.getOutgoingStatus(INVITER).status())
                    .isEqualTo(CallInvitationPollStatus.NONE);
        }

        @Test
        @DisplayName("초대가 없어도 예외 없이 성공한다 (멱등)")
        void cancel_whenNoInvitation_isIdempotent() {
            // when
            callInvitationService.cancel(INVITER);

            // then
            assertThat(callInvitationService.getOutgoingStatus(INVITER).status())
                    .isEqualTo(CallInvitationPollStatus.NONE);
        }
    }

    @Nested
    @DisplayName("accept: 수신 초대 수락")
    class Accept {

        @Test
        @DisplayName("초대를 소비하고 Call을 생성하며 roomId·callId를 반환한다")
        void accept_createsCallAndReturnsIds() {
            // given
            seedInvitableFriend(INVITER, INVITEE);
            callInvitationService.invite(INVITER, INVITEE);

            // when
            final CallInvitationAcceptResponse response = callInvitationService.accept(INVITEE);

            // then
            final Optional<Call> call = callRepository.findByRoomId(response.roomId());
            assertThat(call).isPresent();
            assertThat(call.get().getId()).isEqualTo(response.callId());
            assertThat(call.get().involves(INVITER)).isTrue();
            assertThat(call.get().involves(INVITEE)).isTrue();
            assertThat(call.get().getStartedAt()).isEqualTo(FIXED_NOW);
            assertThat(callInvitationService.findIncoming(INVITEE)).isEmpty();
        }

        @Test
        @DisplayName("초대가 없으면 (만료·취소) 실패한다")
        void accept_whenNoInvitation_throws() {
            assertThatThrownBy(() -> callInvitationService.accept(INVITEE))
                    .isInstanceOf(CallInvitationNotFoundException.class);
        }

        @Test
        @DisplayName("발신자가 취소한 뒤의 수락은 실패한다 — 승자는 하나")
        void accept_afterCancel_throws() {
            // given
            seedInvitableFriend(INVITER, INVITEE);
            callInvitationService.invite(INVITER, INVITEE);
            callInvitationService.cancel(INVITER);

            // when & then
            assertThatThrownBy(() -> callInvitationService.accept(INVITEE))
                    .isInstanceOf(CallInvitationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("decline: 수신 초대 거절")
    class Decline {

        @Test
        @DisplayName("초대를 소비하고 Call을 생성하지 않는다")
        void decline_consumesInvitationWithoutCall() {
            // given
            seedInvitableFriend(INVITER, INVITEE);
            callInvitationService.invite(INVITER, INVITEE);

            // when
            callInvitationService.decline(INVITEE);

            // then
            assertThat(callInvitationService.findIncoming(INVITEE)).isEmpty();
            assertThat(callRepository.count()).isZero();
        }

        @Test
        @DisplayName("초대가 없으면 실패한다")
        void decline_whenNoInvitation_throws() {
            assertThatThrownBy(() -> callInvitationService.decline(INVITEE))
                    .isInstanceOf(CallInvitationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findIncoming: 수신 초대 조회")
    class FindIncoming {

        @Test
        @DisplayName("수신 중인 초대를 반환한다")
        void findIncoming_whenInvited_returnsInvitation() {
            // given
            seedInvitableFriend(INVITER, INVITEE);
            callInvitationService.invite(INVITER, INVITEE);

            // when
            final Optional<CallInvitation> incoming = callInvitationService.findIncoming(INVITEE);

            // then
            assertThat(incoming).isPresent();
            assertThat(incoming.get().inviterId()).isEqualTo(INVITER);
        }

        @Test
        @DisplayName("초대가 없으면 empty를 반환한다")
        void findIncoming_whenNone_returnsEmpty() {
            // when & then
            assertThat(callInvitationService.findIncoming(INVITEE)).isEmpty();
        }
    }
}
