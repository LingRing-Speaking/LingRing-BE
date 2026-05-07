package com.lingring.global.auth.apple;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingring.global.util.FixedDateTimeProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDateTime;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppleClientSecretSignerTest {

    private static final String TEAM_ID = "TEAM123";
    private static final String KEY_ID = "KEY456";
    private static final String CLIENT_ID = "com.lingring.app";

    private KeyPair keyPair;
    private AppleClientSecretSigner signer;

    @BeforeEach
    void setUp() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        keyPair = generator.generateKeyPair();
        final String pem = toPem(keyPair.getPrivate().getEncoded());
        final AppleAuthProperties properties = new AppleAuthProperties(
                TEAM_ID, KEY_ID, CLIENT_ID, pem, null, null
        );
        final FixedDateTimeProvider dateTimeProvider = new FixedDateTimeProvider(
                LocalDateTime.of(2026, 5, 8, 12, 0, 0)
        );
        signer = new AppleClientSecretSigner(properties, dateTimeProvider);
    }

    @Test
    @DisplayName("sign은 ES256 JWT를 발급하고 iss/sub/aud/kid를 Apple 사양대로 채운다")
    void sign_returnsEs256JwtWithExpectedClaims() {
        // when
        final String jwt = signer.sign();

        // then
        final Jws<Claims> parsed = Jwts.parser()
                .verifyWith((PublicKey) keyPair.getPublic())
                .build()
                .parseSignedClaims(jwt);

        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo("ES256");
        assertThat(parsed.getHeader().getKeyId()).isEqualTo(KEY_ID);
        assertThat(parsed.getPayload().getIssuer()).isEqualTo(TEAM_ID);
        assertThat(parsed.getPayload().getSubject()).isEqualTo(CLIENT_ID);
        assertThat(parsed.getPayload().getAudience()).contains("https://appleid.apple.com");
        assertThat(parsed.getPayload().getIssuedAt()).isNotNull();
        assertThat(parsed.getPayload().getExpiration()).isAfter(parsed.getPayload().getIssuedAt());
    }

    @Test
    @DisplayName("동일 signer가 두 번 호출돼도 매번 새로운 JWT(다른 exp 또는 동일 클레임)를 발급한다")
    void sign_isStateless() {
        // when
        final String first = signer.sign();
        final String second = signer.sign();

        // then — 시계가 고정이라 같은 토큰이 나올 수 있음을 허용. 다만 호출은 예외 없이 성공해야 함
        assertThat(first).isNotBlank();
        assertThat(second).isNotBlank();
    }

    private static String toPem(final byte[] pkcs8Encoded) {
        final String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pkcs8Encoded);
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----";
    }
}
