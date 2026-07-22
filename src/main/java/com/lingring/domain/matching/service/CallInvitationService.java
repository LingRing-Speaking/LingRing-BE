package com.lingring.domain.matching.service;

import com.lingring.domain.call.dao.CallRepository;
import com.lingring.domain.call.domain.Call;
import com.lingring.domain.friend.dao.FriendshipRepository;
import com.lingring.domain.friend.domain.Friendship;
import com.lingring.domain.friend.exception.FriendshipNotFoundException;
import com.lingring.domain.matching.dao.CallInvitationRepository;
import com.lingring.domain.matching.dao.dto.InvitationCreateOutcome;
import com.lingring.domain.matching.domain.CallInvitation;
import com.lingring.domain.matching.domain.CallInvitationResult;
import com.lingring.domain.matching.dto.response.CallInvitationAcceptResponse;
import com.lingring.domain.matching.dto.response.CallInvitationStatusResponse;
import com.lingring.domain.matching.exception.CallInvitationAlreadySentException;
import com.lingring.domain.matching.exception.CallInvitationNotFoundException;
import com.lingring.domain.matching.exception.InviteeBusyException;
import com.lingring.domain.matching.exception.InviteeOfflineException;
import com.lingring.domain.matching.exception.SelfCallInvitationException;
import com.lingring.domain.presence.dao.PresenceRepository;
import com.lingring.domain.userevent.event.UserActionEvent;
import com.lingring.global.config.CallInvitationProperties;
import com.lingring.global.util.DateTimeProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CallInvitationService {

    private final CallInvitationRepository callInvitationRepository;
    private final FriendshipRepository friendshipRepository;
    private final PresenceRepository presenceRepository;
    private final CallRepository callRepository;
    private final DateTimeProvider dateTimeProvider;
    private final CallInvitationProperties callInvitationProperties;
    private final ApplicationEventPublisher eventPublisher;

    public void invite(final Long userId, final Long inviteeUserId) {
        if (userId.equals(inviteeUserId)) {
            throw new SelfCallInvitationException(userId);
        }
        validateAcceptedFriendship(userId, inviteeUserId);
        validateInviteeOnline(inviteeUserId);

        final LocalDateTime now = dateTimeProvider.now();
        final CallInvitation invitation = new CallInvitation(
                userId,
                inviteeUserId,
                UUID.randomUUID(),
                now.plus(callInvitationProperties.inviteTtl())
        );
        final InvitationCreateOutcome outcome =
                callInvitationRepository.create(invitation, callInvitationProperties.inviteTtl());
        if (outcome == InvitationCreateOutcome.INVITER_BUSY) {
            throw new CallInvitationAlreadySentException(userId);
        }
        if (outcome == InvitationCreateOutcome.INVITEE_BUSY) {
            throw new InviteeBusyException(inviteeUserId);
        }
        eventPublisher.publishEvent(UserActionEvent.invitationSent(userId, inviteeUserId, now));
    }

    public CallInvitationStatusResponse getOutgoingStatus(final Long userId) {
        final Optional<CallInvitationResult> result = callInvitationRepository.findResult(userId);
        if (result.isPresent()) {
            return CallInvitationStatusResponse.from(result.get());
        }
        if (callInvitationRepository.existsByInviter(userId)) {
            return CallInvitationStatusResponse.ringing();
        }
        return CallInvitationStatusResponse.none();
    }

    public void cancel(final Long userId) {
        final boolean cancelled = callInvitationRepository.cancelByInviter(userId);
        if (!cancelled) {
            return;
        }
        eventPublisher.publishEvent(UserActionEvent.invitationCancelled(userId, dateTimeProvider.now()));
    }

    public CallInvitationAcceptResponse accept(final Long userId) {
        final CallInvitation invitation = callInvitationRepository.claim(userId)
                .orElseThrow(() -> new CallInvitationNotFoundException(userId));
        final LocalDateTime now = dateTimeProvider.now();
        final Call call = callRepository.save(
                Call.start(invitation.inviterId(), userId, invitation.roomId(), now));
        // Call 커밋 후에 결과를 기록해야 발신자가 callId 없는 ACCEPTED를 관측하지 않는다 (#179와 동일 원칙)
        callInvitationRepository.saveResult(
                invitation.inviterId(),
                CallInvitationResult.accepted(invitation.roomId(), call.getId()),
                callInvitationProperties.resultTtl()
        );
        eventPublisher.publishEvent(
                UserActionEvent.invitationAccepted(userId, invitation.inviterId(), invitation.roomId(), now));
        return new CallInvitationAcceptResponse(invitation.roomId(), call.getId());
    }

    public void decline(final Long userId) {
        final CallInvitation invitation = callInvitationRepository.claim(userId)
                .orElseThrow(() -> new CallInvitationNotFoundException(userId));
        callInvitationRepository.saveResult(
                invitation.inviterId(),
                CallInvitationResult.declined(),
                callInvitationProperties.resultTtl()
        );
        eventPublisher.publishEvent(
                UserActionEvent.invitationDeclined(userId, invitation.inviterId(), dateTimeProvider.now()));
    }

    public Optional<CallInvitation> findIncoming(final Long userId) {
        return callInvitationRepository.findByInvitee(userId);
    }

    private void validateAcceptedFriendship(final Long userId, final Long inviteeUserId) {
        final boolean accepted = friendshipRepository.findBetween(userId, inviteeUserId)
                .map(Friendship::isAccepted)
                .orElse(false);
        if (!accepted) {
            throw new FriendshipNotFoundException(userId, inviteeUserId);
        }
    }

    private void validateInviteeOnline(final Long inviteeUserId) {
        final Set<Long> online = presenceRepository.findOnlineUserIds(Set.of(inviteeUserId));
        if (online.isEmpty()) {
            throw new InviteeOfflineException(inviteeUserId);
        }
    }
}
