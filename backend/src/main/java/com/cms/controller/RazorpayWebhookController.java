package com.cms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.service.RazorpayWebhookService;

/** Inbound Razorpay webhook -- unauthenticated (see SecurityConfig's {@code /webhooks/**}
 *  permitAll, same as {@link OneBookWebhookController}), authenticated instead via HMAC-SHA256
 *  signature over the raw request body, never a bearer token. Rejects with 400 on an invalid or
 *  missing signature before any payload parsing happens. */
@RestController
@RequestMapping("/webhooks/razorpay")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final RazorpayWebhookService webhookService;

    public RazorpayWebhookController(RazorpayWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        if (!webhookService.isValidSignature(rawBody, signature)) {
            log.warn("Razorpay webhook rejected -- invalid or missing signature");
            return ResponseEntity.badRequest().build();
        }

        webhookService.processEvent(rawBody);
        return ResponseEntity.ok().build();
    }
}
