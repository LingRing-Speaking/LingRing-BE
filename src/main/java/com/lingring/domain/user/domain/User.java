package com.lingring.domain.user.domain;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import com.lingring.domain.user.domain.vo.Name;
import com.lingring.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Embedded
    private Name name;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    private User(@NonNull final Name name, final String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
    }

    public static User create(@NonNull final Name name, final String profileImage) {
        return new User(name, profileImage);
    }
}
