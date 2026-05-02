package com.lingring.domain.call.dao;

import com.lingring.domain.call.domain.Call;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRepository extends JpaRepository<Call, Long> {

    Optional<Call> findByRoomId(UUID roomId);
}
