package com.cms.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.RazorpayConfig;
import com.cms.dto.CollectPaymentRequest;
import com.cms.model.RazorpayOrder;
import com.cms.model.enums.PaymentMode;
import com.cms.model.enums.RazorpayOrderStatus;
import com.cms.repository.RazorpayOrderRepository;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

/** Processes verified inbound Razorpay webhook deliveries. Mirrors {@code OneBookWebhookService}'s
 *  shape (unauthenticated endpoint, signature/secret verified in the service layer, never trusts an
 *  unverified payload) but with HMAC signature verification instead of a shared-secret header,
 *  matching Razorpay's actual webhook contract. */
@Service
public class RazorpayWebhookService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookService.class);

    private final RazorpayConfig config;
    private final RazorpayOrderRepository razorpayOrderRepository;
    private final PaymentCollectionService paymentCollectionService;

    public RazorpayWebhookService(RazorpayConfig config,
                                   RazorpayOrderRepository razorpayOrderRepository,
                                   PaymentCollectionService paymentCollectionService) {
        this.config = config;
        this.razorpayOrderRepository = razorpayOrderRepository;
        this.paymentCollectionService = paymentCollectionService;
    }

    /** {@code rawBody} must be the exact, unmodified request body bytes-as-string -- Razorpay's
     *  HMAC is computed over the raw payload, not a re-serialized JSON tree. Returns {@code true}
     *  only when the signature is valid; the controller must reject with 400 on {@code false}
     *  without processing anything. */
    public boolean isValidSignature(String rawBody, String signatureHeader) {
        String secret = config.getWebhookSecret();
        if (secret == null || secret.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Razorpay webhook rejected -- webhook secret not configured or signature header missing");
            return false;
        }
        try {
            return Utils.verifyWebhookSignature(rawBody, signatureHeader, secret);
        } catch (RazorpayException e) {
            log.warn("Razorpay webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /** Processes an already signature-verified payload. Only {@code payment.captured} results in a
     *  receipt; every other event is acknowledged and ignored. Idempotent on
     *  {@code razorpay_payment_id} -- a duplicate delivery of the same captured payment creates no
     *  second receipt. */
    @Transactional
    public void processEvent(String rawBody) {
        JSONObject payload = new JSONObject(rawBody);
        String event = payload.optString("event", "");
        if (!"payment.captured".equals(event)) {
            log.debug("Razorpay webhook event '{}' ignored (not payment.captured)", event);
            return;
        }

        JSONObject paymentEntity = payload
            .getJSONObject("payload")
            .getJSONObject("payment")
            .getJSONObject("entity");

        String razorpayPaymentId = paymentEntity.getString("id");
        String razorpayOrderId = paymentEntity.getString("order_id");
        long amountPaise = paymentEntity.getLong("amount");

        if (razorpayOrderRepository.existsByRazorpayPaymentId(razorpayPaymentId)) {
            log.info("Razorpay webhook for payment {} already processed -- skipping duplicate delivery", razorpayPaymentId);
            return;
        }

        RazorpayOrder order = razorpayOrderRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (order == null) {
            log.warn("Razorpay webhook for unknown order {} (payment {}) -- no matching razorpay_orders row",
                razorpayOrderId, razorpayPaymentId);
            return;
        }
        if (order.getStatus() == RazorpayOrderStatus.PAID) {
            log.info("Razorpay order {} already marked PAID -- skipping duplicate delivery", razorpayOrderId);
            return;
        }

        BigDecimal amount = BigDecimal.valueOf(amountPaise).divide(BigDecimal.valueOf(100));

        CollectPaymentRequest request = new CollectPaymentRequest(
            amount, LocalDate.now(), PaymentMode.ONLINE_RAZORPAY,
            razorpayPaymentId, "Online payment via Razorpay (order " + razorpayOrderId + ")",
            null, false);

        try {
            var response = paymentCollectionService.collectPayment(order.getStudent().getId(), request);
            order.setRazorpayPaymentId(razorpayPaymentId);
            order.setReceiptNumber(response.receiptNumber());
            order.setStatus(RazorpayOrderStatus.PAID);
        } catch (RuntimeException e) {
            // Money was captured by Razorpay but the ledger write failed (e.g. the due amount
            // changed between order-creation and webhook delivery) -- mark FAILED for manual
            // reconciliation rather than silently losing the discrepancy. Never rethrow: Razorpay
            // will otherwise keep retrying a delivery that will never succeed.
            log.error("Razorpay payment {} captured but could not be recorded as a receipt for order {}: {}",
                razorpayPaymentId, razorpayOrderId, e.getMessage(), e);
            order.setRazorpayPaymentId(razorpayPaymentId);
            order.setStatus(RazorpayOrderStatus.FAILED);
        }
        razorpayOrderRepository.save(order);
    }
}
