package com.lingring.domain.user.dao;

import com.lingring.domain.user.dao.dto.UserProfileProjection;
import com.lingring.domain.user.dao.dto.UserSearchProjection;
import com.lingring.domain.user.domain.Provider;
import com.lingring.domain.user.domain.User;
import com.lingring.domain.user.domain.vo.Name;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUserId(Provider provider, String providerUserId);

    boolean existsByName(Name name);

    @Query("""
            SELECT u.id AS id,
                   u.name.value AS nickname,
                   u.profileImage.value AS profileImage,
                   s.level AS level,
                   s.mannerTemperature AS mannerTemperature
            FROM User u, UserStats s
            WHERE u.id = :userId AND s.userId = u.id
            """)
    Optional<UserProfileProjection> findProfileById(@Param("userId") Long userId);

    @Query("""
            SELECT u.id AS id,
                   u.name.value AS nickname,
                   u.profileImage.value AS profileImage
            FROM User u
            WHERE u.name.value = :nickname
            """)
    Optional<UserSearchProjection> findSearchProfileByNickname(@Param("nickname") String nickname);
}
