package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.config.RazorpayConfig;
import com.cms.dto.CollectPaymentRequest;
import com.cms.dto.CollectPaymentResponse;
import com.cms.model.RazorpayOrder;
import com.cms.model.Student;
import com.cms.model.enums.PaymentMode;
import com.cms.model.enums.RazorpayOrderStatus;
import com.cms.repository.RazorpayOrderRepository;
import com.razorpay.Utils;

@ExtendWith(MockitoExtension.class)
class RazorpayWebhookServiceTest {

    private static final String SECRET = "whsec_test_12345";

    @Mock private RazorpayOrderRepository razorpayOrderRepository;
    @Mock private PaymentCollectionService paymentCollectionService;

    private RazorpayWebhookService service;
    private RazorpayConfig config;

    @BeforeEach
    void setUp() {
        config = new RazorpayConfig();
        config.setWebhookSecret(SECRET);
        service = new RazorpayWebhookService(config, razorpayOrderRepository, paymentCollectionService);
    }

    private String validSignature(String body) throws Exception {
        return Utils.getHash(body, SECRET);
    }

    private String capturedPayload(String paymentId, String orderId, long amountPaise) {
        return """
            {"event":"payment.captured","payload":{"payment":{"entity":{
              "id":"%s","order_id":"%s","amount":%d
            }}}}""".formatted(paymentId, orderId, amountPaise);
    }

    @Test
    void isValidSignature_acceptsCorrectSignature() throws Exception {
        String body = capturedPayload("pay_1", "order_1", 2500000L);
        assertThat(service.isValidSignature(body, validSignature(body))).isTrue();
    }

    @Test
    void isValidSignature_rejectsTamperedBody() throws Exception {
        String body = capturedPayload("pay_1", "order_1", 2500000L);
        String signature = validSignature(body);
        String tamperedBody = capturedPayload("pay_1", "order_1", 999999999L);

        assertThat(service.isValidSignature(tamperedBody, signature)).isFalse();
    }

    @Test
    void isValidSignature_rejectsMissingHeader() {
        assertThat(service.isValidSignature(capturedPayload("pay_1", "order_1", 100L), null)).isFalse();
    }

    @Test
    void isValidSignature_rejectsWhenSecretNotConfigured() throws Exception {
        config.setWebhookSecret("");
        String body = capturedPayload("pay_1", "order_1", 100L);

        assertThat(service.isValidSignature(body, validSignature(body))).isFalse();
    }

    @Test
    void processEvent_createsReceiptOnCapturedPayment() {
        Student student = new Student();
        student.setId(45L);
        RazorpayOrder order = new RazorpayOrder();
        order.setRazorpayOrderId("order_1");
        order.setStudent(student);
        order.setStatus(RazorpayOrderStatus.CREATED);

        when(razorpayOrderRepository.existsByRazorpayPaymentId("pay_1")).thenReturn(false);
        when(razorpayOrderRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(order));
        when(paymentCollectionService.collectPayment(org.mockito.ArgumentMatchers.eq(45L), any(CollectPaymentRequest.class)))
            .thenReturn(new CollectPaymentResponse(
                "RCP-2026-0099", 45L, "Oviya Thangam", "GNM4-015",
                new BigDecimal("25000.00"), LocalDate.now(), PaymentMode.ONLINE_RAZORPAY,
                "pay_1", null, "Year 1: 25000", java.util.List.of(), "TUITION_ONLY",
                java.time.Instant.now(), BigDecimal.ZERO));

        service.processEvent(capturedPayload("pay_1", "order_1", 2500000L));

        assertThat(order.getStatus()).isEqualTo(RazorpayOrderStatus.PAID);
        assertThat(order.getRazorpayPaymentId()).isEqualTo("pay_1");
        assertThat(order.getReceiptNumber()).isEqualTo("RCP-2026-0099");
        verify(razorpayOrderRepository).save(order);
    }

    @Test
    void processEvent_skipsDuplicateDelivery() {
        when(razorpayOrderRepository.existsByRazorpayPaymentId("pay_1")).thenReturn(true);

        service.processEvent(capturedPayload("pay_1", "order_1", 2500000L));

        verify(razorpayOrderRepository, never()).findByRazorpayOrderId(any());
        verify(paymentCollectionService, never()).collectPayment(any(), any());
    }

    @Test
    void processEvent_ignoresUnknownOrder() {
        when(razorpayOrderRepository.existsByRazorpayPaymentId("pay_1")).thenReturn(false);
        when(razorpayOrderRepository.findByRazorpayOrderId("order_missing")).thenReturn(Optional.empty());

        service.processEvent(capturedPayload("pay_1", "order_missing", 2500000L));

        verify(paymentCollectionService, never()).collectPayment(any(), any());
        verify(razorpayOrderRepository, never()).save(any());
    }

    @Test
    void processEvent_ignoresNonCapturedEvents() {
        String body = """
            {"event":"payment.failed","payload":{"payment":{"entity":{"id":"pay_1","order_id":"order_1","amount":100}}}}""";

        service.processEvent(body);

        verify(razorpayOrderRepository, never()).existsByRazorpayPaymentId(any());
    }

    @Test
    void processEvent_marksOrderFailedWhenLedgerWriteFails() {
        Student student = new Student();
        student.setId(45L);
        RazorpayOrder order = new RazorpayOrder();
        order.setRazorpayOrderId("order_1");
        order.setStudent(student);
        order.setStatus(RazorpayOrderStatus.CREATED);

        when(razorpayOrderRepository.existsByRazorpayPaymentId("pay_1")).thenReturn(false);
        when(razorpayOrderRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(order));
        when(paymentCollectionService.collectPayment(org.mockito.ArgumentMatchers.eq(45L), any(CollectPaymentRequest.class)))
            .thenThrow(new IllegalStateException("No pending fees found"));

        service.processEvent(capturedPayload("pay_1", "order_1", 2500000L));

        assertThat(order.getStatus()).isEqualTo(RazorpayOrderStatus.FAILED);
        assertThat(order.getRazorpayPaymentId()).isEqualTo("pay_1");
        verify(razorpayOrderRepository).save(order);
    }
}
