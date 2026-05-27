package com.lingring.domain.user.dao;

import com.lingring.domain.user.domain.WithdrawalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalLogRepository extends JpaRepository<WithdrawalLog, Long> {
}
