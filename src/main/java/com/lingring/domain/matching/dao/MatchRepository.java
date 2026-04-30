package com.lingring.domain.matching.dao;

import com.lingring.domain.matching.domain.Match;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByRoomId(UUID roomId);
}
