package com.lingring.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestContainersTest
@DisplayName("CORS 통합 테스트")
class CorsIntegrationTest {

    private static final String ORIGIN = "capacitor://localhost";

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("인증 실패(401) 응답에도 Access-Control-Allow-Origin 헤더가 붙는다")
    void protectedEndpoint_whenUnauthenticated_returns401WithCorsHeader() throws Exception {
        // given
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/v1/me")))
                .header("Origin", ORIGIN)
                .GET()
                .build();

        // when
        final HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
                .hasValue(ORIGIN);
    }

    @Test
    @DisplayName("preflight(OPTIONS) 요청에 CORS 허용 헤더를 응답한다")
    void preflight_whenOriginPresent_returnsCorsHeaders() throws Exception {
        // given
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/v1/me")))
                .header("Origin", ORIGIN)
                .header("Access-Control-Request-Method", "GET")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();

        // when
        final HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
                .hasValue(ORIGIN);
    }

    private String url(final String path) {
        return "http://localhost:" + port + path;
    }
}
