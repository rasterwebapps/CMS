package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.OneBookWebhookResult;
import com.cms.service.OneBookWebhookService;

@WebMvcTest(controllers = OneBookWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class OneBookWebhookControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private OneBookWebhookService webhookService;

    private static final String VALID_SECRET = "test-webhook-secret";

    private static final String TRACK_UPDATE_BODY = """
            [{"invoiceNumber":"DSB-2026-001","oneBookPaymentRegisterId":"REG-999","status":"CREATED"}]
            """;

    private static final String TRACK_COMPLETION_BODY = """
            [{"invoiceNumber":"DSB-2026-001","status":"PAID","transactionNumber":"TXN-001"}]
            """;

    @BeforeEach
    void setUp() {
        when(webhookService.isValidSecret(VALID_SECRET)).thenReturn(true);
        when(webhookService.isValidSecret(null)).thenReturn(false);
        when(webhookService.isValidSecret("wrong-secret")).thenReturn(false);
        when(webhookService.toRawJson(any())).thenReturn("{}");
    }

    // ── posting-track-update ──────────────────────────────────────────────────

    @Test
    void postingTrackUpdate_returns200Ack_forValidSecretAndArrayBody() throws Exception {
        when(webhookService.processPostingTrackUpdate(any(), anyString()))
                .thenReturn(OneBookWebhookResult.ok("DSB-2026-001"));

        mockMvc.perform(put("/webhooks/onebook/posting-track-update")
                        .header("X-OneBook-Secret", VALID_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRACK_UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("true"));

        verify(webhookService).processPostingTrackUpdate(any(), anyString());
    }

    @Test
    void postingTrackUpdate_returns200Ack_forSingleObjectBody() throws Exception {
        // OneBook may send a plain object instead of a 1-element array
        when(webhookService.processPostingTrackUpdate(any(), anyString()))
                .thenReturn(OneBookWebhookResult.ok("DSB-2026-001"));

        String singleObject = """
                {"invoiceNumber":"DSB-2026-001","oneBookPaymentRegisterId":"REG-999","status":"CREATED"}
                """;

        mockMvc.perform(put("/webhooks/onebook/posting-track-update")
                        .header("X-OneBook-Secret", VALID_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleObject))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("true"));
    }

    @Test
    void postingTrackUpdate_returns401_whenSecretIsWrong() throws Exception {
        mockMvc.perform(put("/webhooks/onebook/posting-track-update")
                        .header("X-OneBook-Secret", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRACK_UPDATE_BODY))
                .andExpect(status().isUnauthorized());

        verify(webhookService, never()).processPostingTrackUpdate(any(), any());
    }

    @Test
    void postingTrackUpdate_returns401_whenSecretHeaderIsMissing() throws Exception {
        mockMvc.perform(put("/webhooks/onebook/posting-track-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRACK_UPDATE_BODY))
                .andExpect(status().isUnauthorized());

        verify(webhookService, never()).processPostingTrackUpdate(any(), any());
    }

    // ── posting-track-completion ──────────────────────────────────────────────

    @Test
    void postingTrackCompletion_returns200Ack_forValidSecretAndBody() throws Exception {
        when(webhookService.processPostingTrackCompletion(any(), anyString()))
                .thenReturn(OneBookWebhookResult.ok("DSB-2026-001"));

        mockMvc.perform(put("/webhooks/onebook/posting-track-completion")
                        .header("X-OneBook-Secret", VALID_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRACK_COMPLETION_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("true"));

        verify(webhookService).processPostingTrackCompletion(any(), anyString());
    }

    @Test
    void postingTrackCompletion_returns200Ack_forSingleObjectBody() throws Exception {
        when(webhookService.processPostingTrackCompletion(any(), anyString()))
                .thenReturn(OneBookWebhookResult.ok("DSB-2026-001"));

        String singleObject = """
                {"invoiceNumber":"DSB-2026-001","status":"PAID","transactionNumber":"TXN-001"}
                """;

        mockMvc.perform(put("/webhooks/onebook/posting-track-completion")
                        .header("X-OneBook-Secret", VALID_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleObject))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("true"));
    }

    @Test
    void postingTrackCompletion_returns401_whenSecretIsWrong() throws Exception {
        mockMvc.perform(put("/webhooks/onebook/posting-track-completion")
                        .header("X-OneBook-Secret", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRACK_COMPLETION_BODY))
                .andExpect(status().isUnauthorized());

        verify(webhookService, never()).processPostingTrackCompletion(any(), any());
    }

    @Test
    void postingTrackCompletion_returns401_whenSecretHeaderIsMissing() throws Exception {
        mockMvc.perform(put("/webhooks/onebook/posting-track-completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRACK_COMPLETION_BODY))
                .andExpect(status().isUnauthorized());

        verify(webhookService, never()).processPostingTrackCompletion(any(), any());
    }

    // ── non-OK service result is still 200 (OneBook always gets an ACK) ───────

    @Test
    void postingTrackUpdate_returns200_evenWhenServiceReturnsNotFound() throws Exception {
        when(webhookService.processPostingTrackUpdate(any(), anyString()))
                .thenReturn(OneBookWebhookResult.notFound("DSB-GHOST", "unknown"));

        mockMvc.perform(put("/webhooks/onebook/posting-track-update")
                        .header("X-OneBook-Secret", VALID_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRACK_UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("true"));
    }
}
