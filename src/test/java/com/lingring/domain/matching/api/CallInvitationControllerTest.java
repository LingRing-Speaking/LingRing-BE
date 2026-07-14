package com.lingring.domain.matching.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lingring.domain.matching.dto.response.CallInvitationAcceptResponse;
import com.lingring.domain.matching.dto.response.CallInvitationStatusResponse;
import com.lingring.domain.matching.exception.CallInvitationNotFoundException;
import com.lingring.domain.matching.exception.InviteeOfflineException;
import com.lingring.domain.matching.service.CallInvitationService;
import com.lingring.global.auth.context.AuthContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CallInvitationController.class)
class CallInvitationControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CallInvitationService callInvitationService;

    @BeforeEach
    void setAuthContext() {
        AuthContext.set(USER_ID);
    }

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Nested
    @DisplayName("POST /api/v1/call-invitations")
    class Invite {

        @Test
        @DisplayName("초대 생성에 성공하면 204를 반환하고 서비스에 위임한다")
        void invite_returns204AndDelegates() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/call-invitations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"inviteeUserId\": 2}"))
                    .andExpect(status().isNoContent());
            then(callInvitationService).should().invite(USER_ID, 2L);
        }

        @Test
        @DisplayName("inviteeUserId가 없으면 400을 반환한다")
        void invite_whenInviteeUserIdMissing_returns400() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/call-invitations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("수신자가 오프라인이면 409를 반환한다")
        void invite_whenInviteeOffline_returns409() throws Exception {
            // given
            willThrow(new InviteeOfflineException(2L))
                    .given(callInvitationService).invite(USER_ID, 2L);

            // when & then
            mockMvc.perform(post("/api/v1/call-invitations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"inviteeUserId\": 2}"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/call-invitations/outgoing")
    class GetOutgoingStatus {

        @Test
        @DisplayName("발신 상태를 200으로 반환한다")
        void status_returns200WithBody() throws Exception {
            // given
            given(callInvitationService.getOutgoingStatus(USER_ID))
                    .willReturn(CallInvitationStatusResponse.ringing());

            // when & then
            mockMvc.perform(get("/api/v1/call-invitations/outgoing"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("RINGING"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/call-invitations/outgoing")
    class Cancel {

        @Test
        @DisplayName("취소하면 204를 반환하고 서비스에 위임한다")
        void cancel_returns204AndDelegates() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/call-invitations/outgoing"))
                    .andExpect(status().isNoContent());
            then(callInvitationService).should().cancel(USER_ID);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/call-invitations/accept")
    class Accept {

        @Test
        @DisplayName("수락하면 roomId·callId를 200으로 반환한다")
        void accept_returns200WithRoomIdAndCallId() throws Exception {
            // given
            final UUID roomId = UUID.randomUUID();
            given(callInvitationService.accept(USER_ID))
                    .willReturn(new CallInvitationAcceptResponse(roomId, 10L));

            // when & then
            mockMvc.perform(post("/api/v1/call-invitations/accept"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.roomId").value(roomId.toString()))
                    .andExpect(jsonPath("$.data.callId").value(10L));
        }

        @Test
        @DisplayName("수락할 초대가 없으면 404를 반환한다")
        void accept_whenNoInvitation_returns404() throws Exception {
            // given
            given(callInvitationService.accept(USER_ID))
                    .willThrow(new CallInvitationNotFoundException(USER_ID));

            // when & then
            mockMvc.perform(post("/api/v1/call-invitations/accept"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/call-invitations/decline")
    class Decline {

        @Test
        @DisplayName("거절하면 204를 반환하고 서비스에 위임한다")
        void decline_returns204AndDelegates() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/call-invitations/decline"))
                    .andExpect(status().isNoContent());
            then(callInvitationService).should().decline(USER_ID);
        }

        @Test
        @DisplayName("거절할 초대가 없으면 404를 반환한다")
        void decline_whenNoInvitation_returns404() throws Exception {
            // given
            willThrow(new CallInvitationNotFoundException(USER_ID))
                    .given(callInvitationService).decline(USER_ID);

            // when & then
            mockMvc.perform(post("/api/v1/call-invitations/decline"))
                    .andExpect(status().isNotFound());
        }
    }
}
