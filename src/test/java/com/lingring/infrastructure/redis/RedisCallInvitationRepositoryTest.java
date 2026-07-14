package com.lingring.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.matching.dao.CallInvitationRepository;
import com.lingring.domain.matching.dao.dto.InvitationCreateOutcome;
import com.lingring.domain.matching.domain.CallInvitation;
import com.lingring.domain.matching.domain.CallInvitationPollStatus;
import com.lingring.domain.matching.domain.CallInvitationResult;
import com.lingring.global.config.ServiceIntegrationHelper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RedisCallInvitationRepositoryTest extends ServiceIntegrationHelper {

    private static final Duration TTL = Duration.ofSeconds(30);
    private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 5, 12, 12, 0, 30);

    @Autowired
    private CallInvitationRepository callInvitationRepository;

    private CallInvitation invitation(final Long inviterId, final Long inviteeId) {
        return new CallInvitation(inviterId, inviteeId, UUID.randomUUID(), DEADLINE);
    }

    @Nested
    @DisplayName("create는")
    class Create {

        @Test
        @DisplayName("슬롯이 비어 있으면 초대를 생성하고 수신자 조회로 동일한 내용이 나온다")
        void create_whenSlotsFree_storesInvitation() {
            // given
            final CallInvitation invitation = invitation(1L, 2L);

            // when
            final InvitationCreateOutcome outcome = callInvitationRepository.create(invitation, TTL);

            // then
            assertThat(outcome).isEqualTo(InvitationCreateOutcome.CREATED);
            final Optional<CallInvitation> found = callInvitationRepository.findByInvitee(2L);
            assertThat(found).contains(invitation);
            assertThat(callInvitationRepository.existsByInviter(1L)).isTrue();
        }

        @Test
        @DisplayName("발신자가 이미 진행 중인 초대를 갖고 있으면 INVITER_BUSY를 반환한다")
        void create_whenInviterHasOutgoing_returnsInviterBusy() {
            // given
            callInvitationRepository.create(invitation(1L, 2L), TTL);

            // when
            final InvitationCreateOutcome outcome = callInvitationRepository.create(invitation(1L, 3L), TTL);

            // then
            assertThat(outcome).isEqualTo(InvitationCreateOutcome.INVITER_BUSY);
            assertThat(callInvitationRepository.findByInvitee(3L)).isEmpty();
        }

        @Test
        @DisplayName("수신자 슬롯이 점유돼 있으면 INVITEE_BUSY를 반환하고 기존 초대를 보존한다")
        void create_whenInviteeSlotOccupied_returnsInviteeBusy() {
            // given
            final CallInvitation first = invitation(1L, 2L);
            callInvitationRepository.create(first, TTL);

            // when
            final InvitationCreateOutcome outcome = callInvitationRepository.create(invitation(3L, 2L), TTL);

            // then
            assertThat(outcome).isEqualTo(InvitationCreateOutcome.INVITEE_BUSY);
            assertThat(callInvitationRepository.findByInvitee(2L)).contains(first);
        }

        @Test
        @DisplayName("생성 시 발신자의 이전 결과 키를 제거한다")
        void create_clearsStaleResult() {
            // given
            callInvitationRepository.saveResult(1L, CallInvitationResult.declined(), TTL);

            // when
            callInvitationRepository.create(invitation(1L, 2L), TTL);

            // then
            assertThat(callInvitationRepository.findResult(1L)).isEmpty();
        }

        @Test
        @DisplayName("TTL이 지나면 초대가 자동 소멸한다")
        void create_afterTtlExpires_invitationIsGone() throws InterruptedException {
            // given
            callInvitationRepository.create(invitation(1L, 2L), Duration.ofMillis(200));

            // when
            Thread.sleep(400);

            // then
            assertThat(callInvitationRepository.findByInvitee(2L)).isEmpty();
            assertThat(callInvitationRepository.existsByInviter(1L)).isFalse();
        }
    }

    @Nested
    @DisplayName("claim은")
    class Claim {

        @Test
        @DisplayName("초대를 반환하며 소비한다 — 두 번째 claim은 empty")
        void claim_consumesInvitationAtomically() {
            // given
            final CallInvitation invitation = invitation(1L, 2L);
            callInvitationRepository.create(invitation, TTL);

            // when
            final Optional<CallInvitation> first = callInvitationRepository.claim(2L);
            final Optional<CallInvitation> second = callInvitationRepository.claim(2L);

            // then
            assertThat(first).contains(invitation);
            assertThat(second).isEmpty();
            assertThat(callInvitationRepository.findByInvitee(2L)).isEmpty();
        }

        @Test
        @DisplayName("초대가 없으면 empty를 반환한다")
        void claim_whenNoInvitation_returnsEmpty() {
            // when
            final Optional<CallInvitation> claimed = callInvitationRepository.claim(2L);

            // then
            assertThat(claimed).isEmpty();
        }
    }

    @Nested
    @DisplayName("cancelByInviter는")
    class CancelByInviter {

        @Test
        @DisplayName("발신자의 초대를 양쪽 키 모두 제거한다")
        void cancel_removesBothKeys() {
            // given
            callInvitationRepository.create(invitation(1L, 2L), TTL);

            // when
            callInvitationRepository.cancelByInviter(1L);

            // then
            assertThat(callInvitationRepository.findByInvitee(2L)).isEmpty();
            assertThat(callInvitationRepository.existsByInviter(1L)).isFalse();
        }

        @Test
        @DisplayName("초대가 없어도 예외 없이 무시된다 (멱등)")
        void cancel_whenNoInvitation_isIdempotent() {
            // when
            callInvitationRepository.cancelByInviter(1L);

            // then
            assertThat(callInvitationRepository.existsByInviter(1L)).isFalse();
        }

        @Test
        @DisplayName("수신자 슬롯이 제3자의 초대로 바뀌었으면 그 초대를 보존한다")
        void cancel_preservesThirdPartyInvitation() {
            // given: 1L→2L 초대가 소비된 뒤 3L→2L 초대가 생성된 상태
            callInvitationRepository.create(invitation(1L, 2L), TTL);
            callInvitationRepository.claim(2L);
            final CallInvitation thirdParty = invitation(3L, 2L);
            callInvitationRepository.create(thirdParty, TTL);

            // when: 1L의 뒤늦은 취소
            callInvitationRepository.cancelByInviter(1L);

            // then
            assertThat(callInvitationRepository.findByInvitee(2L)).contains(thirdParty);
        }
    }

    @Nested
    @DisplayName("saveResult/findResult는")
    class Result {

        @Test
        @DisplayName("ACCEPTED 결과를 roomId·callId와 함께 왕복 저장한다")
        void result_acceptedRoundTrip() {
            // given
            final UUID roomId = UUID.randomUUID();
            callInvitationRepository.saveResult(1L, CallInvitationResult.accepted(roomId, 10L), TTL);

            // when
            final Optional<CallInvitationResult> found = callInvitationRepository.findResult(1L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().status()).isEqualTo(CallInvitationPollStatus.ACCEPTED);
            assertThat(found.get().roomId()).isEqualTo(roomId);
            assertThat(found.get().callId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("DECLINED 결과를 왕복 저장한다")
        void result_declinedRoundTrip() {
            // given
            callInvitationRepository.saveResult(1L, CallInvitationResult.declined(), TTL);

            // when
            final Optional<CallInvitationResult> found = callInvitationRepository.findResult(1L);

            // then
            assertThat(found).isPresent();
            assertThat(found.get().status()).isEqualTo(CallInvitationPollStatus.DECLINED);
            assertThat(found.get().roomId()).isNull();
            assertThat(found.get().callId()).isNull();
        }

        @Test
        @DisplayName("결과가 없으면 empty를 반환한다")
        void result_whenAbsent_returnsEmpty() {
            // when
            final Optional<CallInvitationResult> found = callInvitationRepository.findResult(1L);

            // then
            assertThat(found).isEmpty();
        }
    }
}
