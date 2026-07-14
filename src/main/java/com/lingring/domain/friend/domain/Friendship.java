package com.lingring.domain.friend.domain;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.friend.exception.FriendshipAccessDeniedException;
import com.lingring.domain.friend.exception.SelfFriendshipException;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(
        name = "friendship",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_friendship_requester_addressee",
                columnNames = {"requester_id", "addressee_id"}
        ),
        indexes = {
                @Index(name = "idx_friendship_requester_id", columnList = "requester_id"),
                @Index(name = "idx_friendship_addressee_id", columnList = "addressee_id")
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Friendship extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "addressee_id", nullable = false)
    private Long addresseeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FriendshipStatus status;

    private Friendship(@NonNull final Long requesterId, @NonNull final Long addresseeId) {
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
        this.status = FriendshipStatus.PENDING;
    }

    public static Friendship request(@NonNull final Long requesterId, @NonNull final Long addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new SelfFriendshipException(requesterId);
        }
        return new Friendship(requesterId, addresseeId);
    }

    public void accept(final Long actorId) {
        if (!addresseeId.equals(actorId)) {
            throw new FriendshipAccessDeniedException(actorId);
        }
        this.status = FriendshipStatus.ACCEPTED;
    }

    public boolean isPending() {
        return status == FriendshipStatus.PENDING;
    }
}
