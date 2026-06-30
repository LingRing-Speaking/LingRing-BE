package com.lingring.domain.matching.dao;

import com.lingring.domain.matching.dao.dto.AcceptResult;
import com.lingring.domain.matching.domain.MatchConfirmation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchConfirmationRepository {

    /**
     * 큐에서 두 사용자를 원자적으로 제거하고 confirm record를 생성한다.
     * 각 사용자의 원래 enqueuedAt을 confirm에 보존해, 만료 시 우선순위 복원에 쓴다.
     * @return true=커밋 성공, false=한쪽이 이미 큐에 없음
     */
    boolean commit(Long userAId, Long userBId, UUID roomId, LocalDateTime deadline,
            LocalDateTime userAEnqueuedAt, LocalDateTime userBEnqueuedAt);

    Optional<MatchConfirmation> findByUser(Long userId);

    /**
     * 사용자의 accept 플래그를 set. 양쪽이 모두 accept하면 result key로 promote.
     */
    AcceptResult accept(Long userId, LocalDateTime now);

    /**
     * pairKey의 confirm record와 양쪽 user index를 삭제한다. (decline/expire 정리)
     */
    void delete(String pairKey);

    List<MatchConfirmation> findAllExpired(LocalDateTime now);
}
