package com.lingring.domain.savedexpression.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lingring.domain.savedexpression.dto.request.SavedExpressionCreateRequest;
import com.lingring.domain.savedexpression.dto.response.SavedExpressionListResponse;
import com.lingring.domain.savedexpression.dto.response.SavedExpressionResponse;
import com.lingring.domain.savedexpression.service.SavedExpressionService;
import java.time.LocalDateTime;
import java.util.List;
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

@WebMvcTest(SavedExpressionController.class)
class SavedExpressionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SavedExpressionService savedExpressionService;

    @Nested
    @DisplayName("POST /users/{userId}/saved-expressions")
    class Create {

        @Test
        @DisplayName("유효한 요청이면 201 응답과 생성된 리소스를 반환한다")
        void create_whenValid_returns201WithBody() throws Exception {
            // given
            final Long userId = 1L;
            final SavedExpressionCreateRequest request =
                    new SavedExpressionCreateRequest("Hello", "안녕");
            given(savedExpressionService.save(eq(userId), any(SavedExpressionCreateRequest.class)))
                    .willReturn(new SavedExpressionResponse(
                            10L, userId, "Hello", "안녕", LocalDateTime.now()
                    ));

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            post("/users/{userId}/saved-expressions", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(201);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            assertThat(body.get("status").asInt()).isEqualTo(201);
            assertThat(body.get("data").get("id").asLong()).isEqualTo(10L);
            assertThat(body.get("data").get("expression").asText()).isEqualTo("Hello");
            assertThat(body.get("data").get("meaning").asText()).isEqualTo("안녕");
        }
    }

    @Nested
    @DisplayName("GET /users/{userId}/saved-expressions")
    class GetAll {

        @Test
        @DisplayName("page/size를 명시하면 200 응답과 items + hasNext를 반환한다")
        void getAll_whenWithParams_returns200WithItemsAndHasNext() throws Exception {
            // given
            final Long userId = 1L;
            given(savedExpressionService.getAllByUserId(userId, 0, 2)).willReturn(
                    new SavedExpressionListResponse(
                            List.of(
                                    new SavedExpressionResponse(2L, userId, "second", "두번째", LocalDateTime.now()),
                                    new SavedExpressionResponse(1L, userId, "first", "첫번째", LocalDateTime.now())
                            ),
                            true
                    )
            );

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            get("/users/{userId}/saved-expressions", userId)
                                    .param("page", "0")
                                    .param("size", "2")
                                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(200);
            final JsonNode body = objectMapper.readTree(response.getContentAsString());
            final JsonNode data = body.get("data");
            assertThat(data.get("items").isArray()).isTrue();
            assertThat(data.get("items").size()).isEqualTo(2);
            assertThat(data.get("items").get(0).get("expression").asText()).isEqualTo("second");
            assertThat(data.get("hasNext").asBoolean()).isTrue();
        }

        @Test
        @DisplayName("page/size를 생략하면 default(0, 20)로 서비스가 호출된다")
        void getAll_whenNoParams_usesDefaults() throws Exception {
            // given
            final Long userId = 1L;
            given(savedExpressionService.getAllByUserId(userId, 0, 20)).willReturn(
                    new SavedExpressionListResponse(List.of(), false)
            );

            // when
            mockMvc.perform(get("/users/{userId}/saved-expressions", userId)
                    .accept(MediaType.APPLICATION_JSON));

            // then
            then(savedExpressionService).should().getAllByUserId(userId, 0, 20);
        }
    }

    @Nested
    @DisplayName("DELETE /users/{userId}/saved-expressions/{id}")
    class Delete {

        @Test
        @DisplayName("삭제 성공 시 204 응답을 반환한다")
        void delete_whenSuccess_returns204() throws Exception {
            // given
            final Long userId = 1L;
            final Long id = 10L;
            willDoNothing().given(savedExpressionService).delete(userId, id);

            // when
            final MockHttpServletResponse response = mockMvc.perform(
                            delete("/users/{userId}/saved-expressions/{id}", userId, id))
                    .andReturn()
                    .getResponse();

            // then
            assertThat(response.getStatus()).isEqualTo(204);
            then(savedExpressionService).should().delete(userId, id);
        }
    }
}
