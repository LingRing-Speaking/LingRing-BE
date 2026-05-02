package com.lingring.domain.userreport.dao;

import com.lingring.domain.userreport.domain.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserReport ur SET ur.userId = null WHERE ur.userId = :userId")
    int anonymizeReporter(@Param("userId") Long userId);
}
