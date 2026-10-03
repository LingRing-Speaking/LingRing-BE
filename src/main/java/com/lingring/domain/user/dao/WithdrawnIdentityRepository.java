package com.lingring.domain.user.dao;

import com.lingring.domain.user.domain.WithdrawnIdentity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WithdrawnIdentityRepository extends JpaRepository<WithdrawnIdentity, Long> {

    Optional<WithdrawnIdentity> findByIdentityHash(String identityHash);

    boolean existsByIdentityHash(String identityHash);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM WithdrawnIdentity w WHERE w.withdrawnAt < :threshold")
    int deleteAllWithdrawnBefore(@Param("threshold") LocalDateTime threshold);
}
