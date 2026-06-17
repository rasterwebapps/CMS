package com.cms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.OneBookWebhookPayload;
import com.cms.service.OneBookWebhookService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/webhooks/onebook")
public class OneBookWebhookController {

    private static final Logger log = LoggerFactory.getLogger(OneBookWebhookController.class);

    private final OneBookWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public OneBookWebhookController(OneBookWebhookService webhookService, ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/payment-status")
    public ResponseEntity<Void> paymentStatus(
            @RequestHeader(value = "X-OneBook-Secret", required = false) String secret,
            @RequestBody OneBookWebhookPayload payload) {

        if (!webhookService.isValidSecret(secret)) {
            log.warn("OneBook webhook rejected — invalid or missing X-OneBook-Secret");
            return ResponseEntity.status(401).build();
        }

        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            rawJson = "{}";
        }

        try {
            webhookService.process(payload, rawJson);
        } catch (EntityNotFoundException e) {
            log.warn("OneBook webhook: {}", e.getMessage());
            return ResponseEntity.status(422).build();
        } catch (Exception e) {
            log.error("OneBook webhook processing failed for ref={}: {}", payload.referenceId(), e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }

        return ResponseEntity.ok().build();
    }
}
