package com.lingring.domain.friend.dao;

import com.lingring.domain.friend.dao.dto.FriendItemProjection;
import com.lingring.domain.friend.domain.Friendship;
import com.lingring.domain.friend.domain.FriendshipStatus;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("""
            SELECT f
            FROM Friendship f
            WHERE (f.requesterId = :userId AND f.addresseeId = :otherUserId)
               OR (f.requesterId = :otherUserId AND f.addresseeId = :userId)
            """)
    Optional<Friendship> findBetween(
            @Param("userId") Long userId,
            @Param("otherUserId") Long otherUserId
    );

    @Query("""
            SELECT (CASE WHEN f.requesterId = :userId THEN f.addresseeId ELSE f.requesterId END) AS userId,
                   u.name.value AS nickname,
                   u.profileImage.value AS profileImage,
                   f.status AS status,
                   (CASE WHEN f.requesterId = :userId THEN 'SENT' ELSE 'RECEIVED' END) AS direction,
                   f.createdAt AS requestedAt
            FROM Friendship f
            LEFT JOIN User u ON u.id = (CASE WHEN f.requesterId = :userId THEN f.addresseeId ELSE f.requesterId END)
            WHERE ((:includeSent = TRUE AND f.requesterId = :userId)
                OR (:includeReceived = TRUE AND f.addresseeId = :userId))
              AND f.status = :status
            ORDER BY f.updatedAt DESC
            """)
    Slice<FriendItemProjection> findItemsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") FriendshipStatus status,
            @Param("includeSent") boolean includeSent,
            @Param("includeReceived") boolean includeReceived,
            Pageable pageable
    );

    long countByAddresseeIdAndStatus(Long addresseeId, FriendshipStatus status);
}
