package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.service.RazorpayWebhookService;

@WebMvcTest(controllers = RazorpayWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class RazorpayWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RazorpayWebhookService webhookService;

    private static final String BODY = """
        {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_1","order_id":"order_1","amount":100}}}}""";

    @Test
    void acceptsAndProcessesValidSignature() throws Exception {
        when(webhookService.isValidSignature(BODY, "sig123")).thenReturn(true);

        mockMvc.perform(post("/webhooks/razorpay")
                .header("X-Razorpay-Signature", "sig123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(status().isOk());

        verify(webhookService).processEvent(BODY);
    }

    @Test
    void rejectsInvalidSignatureWithoutProcessing() throws Exception {
        when(webhookService.isValidSignature(BODY, "bad-sig")).thenReturn(false);

        mockMvc.perform(post("/webhooks/razorpay")
                .header("X-Razorpay-Signature", "bad-sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(status().isBadRequest());

        verify(webhookService, never()).processEvent(any());
    }

    @Test
    void rejectsMissingSignatureHeaderWithoutProcessing() throws Exception {
        when(webhookService.isValidSignature(BODY, null)).thenReturn(false);

        mockMvc.perform(post("/webhooks/razorpay")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(status().isBadRequest());

        verify(webhookService, never()).processEvent(any());
    }
}
