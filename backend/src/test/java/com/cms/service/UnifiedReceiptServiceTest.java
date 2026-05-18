package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.UnifiedReceiptResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.PaymentReceipt;
import com.cms.repository.PaymentReceiptRepository;

@ExtendWith(MockitoExtension.class)
class UnifiedReceiptServiceTest {

    @Mock
    private PaymentReceiptRepository receiptRepository;

    @Mock
    private ApplicationNumberSequenceService numberSequenceService;

    private UnifiedReceiptService service;

    @BeforeEach
    void setUp() {
        service = new UnifiedReceiptService(receiptRepository, numberSequenceService);
    }

    // ─── generateReceiptNumber ────────────────────────────────────────────────

    @Test
    void generateReceiptNumberDelegatesToSequenceService() {
        int year = LocalDate.now().getYear();
        when(numberSequenceService.nextReceiptNumber(year)).thenReturn("RCP-2026-00001");

        String result = service.generateReceiptNumber();

        assertThat(result).isEqualTo("RCP-2026-00001");
        verify(numberSequenceService).nextReceiptNumber(year);
    }

    // ─── saveStudentReceipt ───────────────────────────────────────────────────

    @Test
    void saveStudentReceiptPersistsReceipt() {
        when(receiptRepository.save(any(PaymentReceipt.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        service.saveStudentReceipt(
            "RCP-2026-00001",
            1L, "Ravi Kumar", "CS2401", "ADM-2601-0001",
            "B.Sc. Computer Science", BigDecimal.valueOf(25000),
            LocalDate.of(2026, 1, 15), "CASH",
            null, "Semester fee", "Semester 1", "admin");

        verify(receiptRepository).save(any(PaymentReceipt.class));
    }

    // ─── saveEnquiryReceipt ───────────────────────────────────────────────────

    @Test
    void saveEnquiryReceiptPersistsReceipt() {
        when(receiptRepository.save(any(PaymentReceipt.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        service.saveEnquiryReceipt(
            "RCP-2026-00002",
            10L, "Priya S", "B.Tech IT", BigDecimal.valueOf(1000),
            LocalDate.of(2026, 2, 1), "ONLINE",
            "TXN123", "Enquiry fee", null, "admin");

        verify(receiptRepository).save(any(PaymentReceipt.class));
    }

    // ─── getAllReceipts ───────────────────────────────────────────────────────

    @Test
    void getAllReceiptsReturnsMappedResponses() {
        PaymentReceipt receipt = buildReceipt("RCP-2026-00001", "STUDENT", 1L);
        when(receiptRepository.findAllByOrderByCreatedAtDescIdDesc())
            .thenReturn(List.of(receipt));

        List<UnifiedReceiptResponse> results = service.getAllReceipts();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).receiptNumber()).isEqualTo("RCP-2026-00001");
        assertThat(results.get(0).payerType()).isEqualTo("STUDENT");
    }

    @Test
    void getAllReceiptsReturnsEmptyListWhenNoReceipts() {
        when(receiptRepository.findAllByOrderByCreatedAtDescIdDesc())
            .thenReturn(List.of());

        List<UnifiedReceiptResponse> results = service.getAllReceipts();

        assertThat(results).isEmpty();
    }

    // ─── getReceiptByNumber ───────────────────────────────────────────────────

    @Test
    void getReceiptByNumberReturnsReceiptWhenFound() {
        PaymentReceipt receipt = buildReceipt("RCP-2026-00001", "STUDENT", 1L);
        when(receiptRepository.findByReceiptNumber("RCP-2026-00001"))
            .thenReturn(Optional.of(receipt));

        UnifiedReceiptResponse result = service.getReceiptByNumber("RCP-2026-00001");

        assertThat(result.receiptNumber()).isEqualTo("RCP-2026-00001");
    }

    @Test
    void getReceiptByNumberThrowsWhenNotFound() {
        when(receiptRepository.findByReceiptNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReceiptByNumber("UNKNOWN"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("UNKNOWN");
    }

    // ─── getReceiptsForPayer ──────────────────────────────────────────────────

    @Test
    void getReceiptsForPayerReturnsMappedList() {
        PaymentReceipt r1 = buildReceipt("RCP-2026-00001", "STUDENT", 1L);
        PaymentReceipt r2 = buildReceipt("RCP-2026-00002", "STUDENT", 1L);
        when(receiptRepository.findByPayerTypeAndPayerIdOrderByCreatedAtDesc("STUDENT", 1L))
            .thenReturn(List.of(r1, r2));

        List<UnifiedReceiptResponse> results = service.getReceiptsForPayer("STUDENT", 1L);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).receiptNumber()).isEqualTo("RCP-2026-00001");
    }

    @Test
    void getReceiptsForPayerReturnsEmptyWhenNone() {
        when(receiptRepository.findByPayerTypeAndPayerIdOrderByCreatedAtDesc("ENQUIRY", 99L))
            .thenReturn(List.of());

        List<UnifiedReceiptResponse> results = service.getReceiptsForPayer("ENQUIRY", 99L);

        assertThat(results).isEmpty();
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private PaymentReceipt buildReceipt(String receiptNumber, String payerType, Long payerId) {
        return new PaymentReceipt(
            receiptNumber, payerType, payerId,
            "Test Payer", "ROLL001", "ADM-26-001", "B.Sc CS",
            BigDecimal.valueOf(10000), LocalDate.now(), "CASH",
            null, null, null, "admin");
    }
}

