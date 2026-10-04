package com.lingring.domain.referral.dao;

import com.lingring.domain.referral.domain.ReferralRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReferralRedemptionRepository extends JpaRepository<ReferralRedemption, Long> {

    boolean existsByInviteeId(Long inviteeId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ReferralRedemption r WHERE r.inviteeId = :inviteeId")
    int deleteByInviteeId(@Param("inviteeId") Long inviteeId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ReferralRedemption r SET r.referrerId = null WHERE r.referrerId = :referrerId")
    int anonymizeReferrer(@Param("referrerId") Long referrerId);
}
