package com.lingring.infrastructure.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

    private static final String APP_NAME_PREFIX = "lingring-";

    // FirebaseApp은 JVM 전역 레지스트리에 이름으로 등록되므로 컨텍스트마다 고유 이름을 쓰고 종료 시 해제한다
    @Bean(destroyMethod = "delete")
    public FirebaseApp firebaseApp(final FirebaseProperties properties) throws IOException {
        final byte[] serviceAccountJson = Base64.getDecoder().decode(properties.credentials());
        final FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountJson)))
                .build();
        return FirebaseApp.initializeApp(options, APP_NAME_PREFIX + UUID.randomUUID());
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(final FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
