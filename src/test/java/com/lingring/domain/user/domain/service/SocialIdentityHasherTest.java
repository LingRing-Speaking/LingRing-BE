package com.lingring.domain.user.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.domain.user.domain.Provider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SocialIdentityHasherTest {

    private final SocialIdentityHasher hasher = new SocialIdentityHasher("test-hmac-key");

    @Test
    @DisplayName("같은 provider와 sub는 항상 같은 해시를 만든다")
    void hash_whenSameIdentity_returnsSameHash() {
        // when
        final String first = hasher.hash(Provider.KAKAO, "12345");
        final String second = hasher.hash(Provider.KAKAO, "12345");

        // then
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("sub가 같아도 provider가 다르면 다른 해시를 만든다")
    void hash_whenProviderDiffers_returnsDifferentHash() {
        // when
        final String kakao = hasher.hash(Provider.KAKAO, "12345");
        final String apple = hasher.hash(Provider.APPLE, "12345");

        // then
        assertThat(kakao).isNotEqualTo(apple);
    }

    @Test
    @DisplayName("키가 다르면 같은 입력이라도 다른 해시를 만든다")
    void hash_whenKeyDiffers_returnsDifferentHash() {
        // given
        final SocialIdentityHasher otherKeyHasher = new SocialIdentityHasher("other-hmac-key");

        // when
        final String original = hasher.hash(Provider.KAKAO, "12345");
        final String other = otherKeyHasher.hash(Provider.KAKAO, "12345");

        // then
        assertThat(original).isNotEqualTo(other);
    }

    @Test
    @DisplayName("해시는 소문자 hex 64자이며 원본 sub를 포함하지 않는다")
    void hash_returns64LowercaseHexWithoutRawSub() {
        // when
        final String hash = hasher.hash(Provider.KAKAO, "12345");

        // then
        assertThat(hash).matches("[0-9a-f]{64}");
        assertThat(hash).doesNotContain("12345");
    }
}
