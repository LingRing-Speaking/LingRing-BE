package com.lingring.domain.user.domain;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.user.domain.vo.Name;
import com.lingring.domain.user.domain.vo.ProfileImage;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_provider_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                ),
                @UniqueConstraint(
                        name = "uk_users_name",
                        columnNames = {"name"}
                )
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private Provider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Embedded
    private Name name;

    @Embedded
    private ProfileImage profileImage;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    @Column(name = "agreed_terms_version", length = 20)
    private String agreedTermsVersion;

    private User(
            @NonNull final Provider provider,
            @NonNull final String providerUserId,
            @NonNull final Name name,
            final ProfileImage profileImage
    ) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.name = name;
        this.profileImage = profileImage;
    }

    public static User createFromOAuth(
            @NonNull final Provider provider,
            @NonNull final String providerUserId,
            @NonNull final Name name,
            final String profileImageUrl
    ) {
        return new User(provider, providerUserId, name, ProfileImage.fromNullable(profileImageUrl));
    }

    public void changeName(@NonNull final Name newName) {
        this.name = newName;
    }

    public void changeProfileImage(@NonNull final ProfileImage newProfileImage) {
        this.profileImage = newProfileImage;
    }

    public void markAgreed(@NonNull final String termsVersion, @NonNull final LocalDateTime agreedAt) {
        this.agreedTermsVersion = termsVersion;
        this.agreedAt = agreedAt;
    }

    public boolean requiresOnboarding() {
        return this.agreedAt == null;
    }
}
