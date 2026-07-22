package com.lingring.domain.matching.dao;

import com.lingring.domain.matching.dao.dto.InvitationCreateOutcome;
import com.lingring.domain.matching.domain.CallInvitation;
import com.lingring.domain.matching.domain.CallInvitationResult;
import java.time.Duration;
import java.util.Optional;

public interface CallInvitationRepository {

    // 원자적 생성: out/in 슬롯이 모두 비어 있어야 성공하며, 발신자의 이전 결과 키도 함께 제거된다
    InvitationCreateOutcome create(CallInvitation invitation, Duration ttl);

    Optional<CallInvitation> findByInvitee(Long inviteeId);

    // 원자적 소비(수락/거절 공용): 만료·취소로 이미 없으면 empty — 승자는 항상 하나
    Optional<CallInvitation> claim(Long inviteeId);

    boolean existsByInviter(Long inviterId);

    // 자신이 만든 초대만 제거 (같은 수신자에게 도착한 제3자의 초대 보호), 멱등. 발신 초대가 실제 존재했으면 true
    boolean cancelByInviter(Long inviterId);

    void saveResult(Long inviterId, CallInvitationResult result, Duration ttl);

    Optional<CallInvitationResult> findResult(Long inviterId);
}
