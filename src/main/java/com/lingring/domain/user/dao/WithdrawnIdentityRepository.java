package com.lingring.domain.user.dao;

import com.lingring.domain.user.domain.WithdrawnIdentity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawnIdentityRepository extends JpaRepository<WithdrawnIdentity, Long> {

    Optional<WithdrawnIdentity> findByIdentityHash(String identityHash);

    boolean existsByIdentityHash(String identityHash);
}
