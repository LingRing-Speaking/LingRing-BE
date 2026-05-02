package com.lingring.domain.call.dao;

import com.lingring.domain.call.domain.CallHistory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallHistoryRepository extends JpaRepository<CallHistory, Long> {

    Optional<CallHistory> findByRoomId(UUID roomId);
}
