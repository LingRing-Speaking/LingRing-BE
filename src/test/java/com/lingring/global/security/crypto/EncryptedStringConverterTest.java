package com.lingring.global.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EncryptedStringConverterTest {

    private static final String TEST_PASSWORD = "test-encrypt-password";
    private static final String TEST_SALT = "deadbeefcafebabe";

    private final EncryptedStringConverter converter = new EncryptedStringConverter(TEST_PASSWORD, TEST_SALT);

    @Test
    @DisplayName("convertToDatabaseColumn → convertToEntityAttribute 라운드트립이 원문을 그대로 복원한다")
    void convertToDatabaseColumn_roundTrip_recoversOriginal() {
        // given
        final String plaintext = "apple-refresh-token-secret-value";

        // when
        final String encrypted = converter.convertToDatabaseColumn(plaintext);
        final String decrypted = converter.convertToEntityAttribute(encrypted);

        // then
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("같은 입력이라도 매번 IV가 달라 암호문이 달라야 한다 (semantic security)")
    void convertToDatabaseColumn_sameInputProducesDifferentCiphertext() {
        // given
        final String plaintext = "apple-refresh-token-secret-value";

        // when
        final String first = converter.convertToDatabaseColumn(plaintext);
        final String second = converter.convertToDatabaseColumn(plaintext);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("null은 그대로 null로 통과한다")
    void convertToDatabaseColumn_nullPassesThrough() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
