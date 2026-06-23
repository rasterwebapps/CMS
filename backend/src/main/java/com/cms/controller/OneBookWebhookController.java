package com.cms.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.dto.OneBookPostingTrackCompletionPayload;
import com.cms.dto.OneBookPostingTrackUpdatePayload;
import com.cms.dto.OneBookWebhookResult;
import com.cms.service.OneBookWebhookService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Inbound callbacks from OneBook — matches the real OneBook API contract,
 * which calls back into the source application's own endpoints rather than
 * returning the register id synchronously or posting to one generic webhook.
 * Per OneBook's documented request shape, the body is always a JSON array
 * (even for a single register); the documented response is a flat
 * {"message": "true"} ack rather than a per-entry result array.
 */
@RestController
@RequestMapping("/webhooks/onebook")
public class OneBookWebhookController {

    private static final Logger log = LoggerFactory.getLogger(OneBookWebhookController.class);
    private static final Map<String, Object> ACK = Map.of("message", "true");

    private final OneBookWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public OneBookWebhookController(OneBookWebhookService webhookService, ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    /** OneBook calls this right after accepting a payment register, to deliver its assigned id. */
    @PutMapping("/posting-track-update")
    public ResponseEntity<Map<String, Object>> postingTrackUpdate(
            @RequestHeader(value = "X-OneBook-Secret", required = false) String secret,
            @RequestBody JsonNode body) {

        if (!webhookService.isValidSecret(secret)) {
            log.warn("OneBook posting-track-update rejected — invalid or missing X-OneBook-Secret");
            return ResponseEntity.status(401).build();
        }

        List<OneBookPostingTrackUpdatePayload> payloads;
        try {
            payloads = parse(body, OneBookPostingTrackUpdatePayload.class);
        } catch (IllegalArgumentException e) {
            log.warn("OneBook posting-track-update payload could not be parsed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        for (OneBookPostingTrackUpdatePayload payload : payloads) {
            OneBookWebhookResult result = webhookService.processPostingTrackUpdate(
                    payload, webhookService.toRawJson(payload));
            if (!"OK".equals(result.status())) {
                log.warn("OneBook posting-track-update entry not processed: {} — {}", result.status(), result.message());
            }
        }
        return ResponseEntity.ok(ACK);
    }

    /** OneBook calls this once the payment register's payment is actually completed (or fails). */
    @PutMapping("/posting-track-completion")
    public ResponseEntity<Map<String, Object>> postingTrackCompletion(
            @RequestHeader(value = "X-OneBook-Secret", required = false) String secret,
            @RequestBody JsonNode body) {

        if (!webhookService.isValidSecret(secret)) {
            log.warn("OneBook posting-track-completion rejected — invalid or missing X-OneBook-Secret");
            return ResponseEntity.status(401).build();
        }

        List<OneBookPostingTrackCompletionPayload> payloads;
        try {
            payloads = parse(body, OneBookPostingTrackCompletionPayload.class);
        } catch (IllegalArgumentException e) {
            log.warn("OneBook posting-track-completion payload could not be parsed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        for (OneBookPostingTrackCompletionPayload payload : payloads) {
            OneBookWebhookResult result = webhookService.processPostingTrackCompletion(
                    payload, webhookService.toRawJson(payload));
            if (!"OK".equals(result.status())) {
                log.warn("OneBook posting-track-completion entry not processed: {} — {}", result.status(), result.message());
            }
        }
        return ResponseEntity.ok(ACK);
    }

    private <T> List<T> parse(JsonNode body, Class<T> type) {
        if (body.isArray()) {
            return objectMapper.convertValue(body, objectMapper.getTypeFactory().constructCollectionType(List.class, type));
        }
        return List.of(objectMapper.convertValue(body, type));
    }
}
