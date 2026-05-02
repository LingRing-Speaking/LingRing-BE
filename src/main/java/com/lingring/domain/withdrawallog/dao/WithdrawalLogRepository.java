package com.lingring.domain.withdrawallog.dao;

import com.lingring.domain.withdrawallog.domain.WithdrawalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalLogRepository extends JpaRepository<WithdrawalLog, Long> {
}
