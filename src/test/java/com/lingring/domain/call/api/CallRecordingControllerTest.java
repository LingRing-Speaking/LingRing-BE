package com.lingring.domain.call.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.call.domain.CallRecording;
import com.lingring.domain.call.dto.request.CallRecordingCreateRequest;
import com.lingring.domain.call.dto.request.CallRecordingPresignedUrlRequest;
import com.lingring.domain.call.dto.response.CallRecordingPresignedUrlResponse;
import com.lingring.domain.call.service.CallRecordingCreateResult;
import com.lingring.domain.call.service.CallRecordingService;
import com.lingring.global.auth.context.AuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CallRecordingController.class)
class CallRecordingControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long CALL_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CallRecordingService callRecordingService;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("POST /api/v1/calls/{callId}/recordings/presigned-url")
    class CreatePresignedUrl {

        @Test
        @DisplayName("발급에 성공하면 200과 url/key를 반환한다")
        void createPresignedUrl_returns200() throws Exception {
            // given
            AuthContext.set(USER_ID);
            final String url = "https://fake-s3.test/upload/abc";
            final String key = "call-recordings/42/1/abc";
            given(callRecordingService.createPresignedUrl(eq(CALL_ID), eq(USER_ID), any()))
                    .willReturn(new CallRecordingPresignedUrlResponse(url, key));
            final String body = """
                    {"contentType":"audio/m4a","contentLength":1024}
                    """;

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/calls/{callId}/recordings/presigned-url", CALL_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("url").asText()).isEqualTo(url);
            assertThat(data.get("key").asText()).isEqualTo(key);
        }

        @Test
        @DisplayName("contentType이 비어 있으면 body status=400을 반환한다")
        void createPresignedUrl_whenContentTypeBlank_returnsValidationError() throws Exception {
            // given
            AuthContext.set(USER_ID);
            final String body = """
                    {"contentType":"","contentLength":1024}
                    """;

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/calls/{callId}/recordings/presigned-url", CALL_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode responseBody = objectMapper.readTree(response.getContentAsString());
            assertThat(responseBody.get("status").asInt()).isEqualTo(400);
        }

        @Test
        @DisplayName("contentLength가 0이면 body status=400을 반환한다")
        void createPresignedUrl_whenContentLengthZero_returnsValidationError() throws Exception {
            // given
            AuthContext.set(USER_ID);
            final String body = """
                    {"contentType":"audio/m4a","contentLength":0}
                    """;

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/calls/{callId}/recordings/presigned-url", CALL_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode responseBody = objectMapper.readTree(response.getContentAsString());
            assertThat(responseBody.get("status").asInt()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/calls/{callId}/recordings")
    class Create {

        @Test
        @DisplayName("신규 생성이면 201과 Location 헤더를 반환한다")
        void create_whenNew_returns201WithLocation() throws Exception {
            // given
            AuthContext.set(USER_ID);
            final CallRecording recording = CallRecording.upload(CALL_ID, USER_ID,
                    "call-recordings/42/1/abc", "audio/m4a");
            setId(recording, 7L);
            given(callRecordingService.create(eq(CALL_ID), eq(USER_ID), any()))
                    .willReturn(CallRecordingCreateResult.created(recording));
            final String body = objectMapper.writeValueAsString(
                    new CallRecordingCreateRequest("call-recordings/42/1/abc"));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/calls/{callId}/recordings", CALL_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(201);
            assertThat(response.getHeader("Location")).isEqualTo("/calls/42/recordings/7");
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("recordingId").asLong()).isEqualTo(7L);
            assertThat(data.get("status").asText()).isEqualTo("UPLOADED");
        }

        @Test
        @DisplayName("멱등 재요청이면 200을 반환하고 Location 헤더가 없다")
        void create_whenIdempotent_returns200WithoutLocation() throws Exception {
            // given
            AuthContext.set(USER_ID);
            final CallRecording recording = CallRecording.upload(CALL_ID, USER_ID,
                    "call-recordings/42/1/abc", "audio/m4a");
            setId(recording, 7L);
            given(callRecordingService.create(eq(CALL_ID), eq(USER_ID), any()))
                    .willReturn(CallRecordingCreateResult.existing(recording));
            final String body = objectMapper.writeValueAsString(
                    new CallRecordingCreateRequest("call-recordings/42/1/abc"));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/calls/{callId}/recordings", CALL_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("Location")).isNull();
            final JsonNode data = objectMapper.readTree(response.getContentAsString()).get("data");
            assertThat(data.get("recordingId").asLong()).isEqualTo(7L);
        }

        @Test
        @DisplayName("recordingKey가 비어 있으면 body status=400을 반환한다")
        void create_whenRecordingKeyBlank_returnsValidationError() throws Exception {
            // given
            AuthContext.set(USER_ID);
            final String body = """
                    {"recordingKey":""}
                    """;

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/api/v1/calls/{callId}/recordings", CALL_ID)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andReturn()
                    .getResponse();

            // then
            final JsonNode responseBody = objectMapper.readTree(response.getContentAsString());
            assertThat(responseBody.get("status").asInt()).isEqualTo(400);
        }
    }

    private static void setId(final CallRecording recording, final Long id) {
        try {
            final var field = CallRecording.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(recording, id);
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
