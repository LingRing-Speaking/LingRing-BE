package com.lingring.global.auth.apple;

import com.lingring.global.error.ErrorCode;
import com.lingring.global.error.exception.InternalServerException;
import com.lingring.global.util.DateTimeProvider;
import io.jsonwebtoken.Jwts;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppleClientSecretSigner {

    private static final String AUDIENCE = "https://appleid.apple.com";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final long TTL_SECONDS = 300L;

    private final AppleAuthProperties properties;
    private final DateTimeProvider dateTimeProvider;

    public String sign() {
        final Instant now = dateTimeProvider.now().atZone(ZONE).toInstant();
        return Jwts.builder()
                .header().keyId(properties.keyId()).and()
                .issuer(properties.teamId())
                .subject(properties.clientId())
                .audience().add(AUDIENCE).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(TTL_SECONDS)))
                .signWith(parsePrivateKey(properties.privateKey()), Jwts.SIG.ES256)
                .compact();
    }

    private PrivateKey parsePrivateKey(final String pem) {
        if (pem == null || pem.isBlank()) {
            throw new InternalServerException(
                    ErrorCode.SERVER_ERROR,
                    "Apple private key가 설정되지 않았습니다."
            );
        }
        final String cleaned = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            final byte[] decoded = Base64.getDecoder().decode(cleaned);
            final KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (final GeneralSecurityException | IllegalArgumentException ex) {
            throw new InternalServerException(
                    ErrorCode.SERVER_ERROR,
                    "Apple private key 파싱에 실패했습니다.",
                    ex
            );
        }
    }
}
