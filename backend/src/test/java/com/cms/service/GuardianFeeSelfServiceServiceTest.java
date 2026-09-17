package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.cms.dto.PenaltyResponse;
import com.cms.dto.ReceiptResponse;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class GuardianFeeSelfServiceServiceTest {

    @Mock
    private FeeFinalizationService feeFinalizationService;
    @Mock
    private PaymentCollectionService paymentCollectionService;
    @Mock
    private PenaltyCalculationService penaltyCalculationService;

    private GuardianFeeSelfServiceService service;

    @BeforeEach
    void setUp() {
        service = new GuardianFeeSelfServiceService(
            feeFinalizationService, paymentCollectionService, penaltyCalculationService);
    }

    @Test
    void findWardSummary_delegatesByStudentId() {
        StudentFeeAllocationResponse response = allocationResponse();
        when(feeFinalizationService.getByStudentId(45L)).thenReturn(response);

        assertThat(service.findWardSummary(45L)).contains(response);
    }

    @Test
    void findWardSummary_emptyNotErrorWhenNoAllocationFinalizedYet() {
        when(feeFinalizationService.getByStudentId(45L))
            .thenThrow(new ResourceNotFoundException("Fee allocation not found for student id: 45"));

        assertThat(service.findWardSummary(45L)).isEmpty();
    }

    @Test
    void findWardReceipts_delegatesByStudentId() {
        List<ReceiptResponse> receipts = List.of(receiptResponse());
        when(paymentCollectionService.getReceipts(45L)).thenReturn(receipts);

        assertThat(service.findWardReceipts(45L)).isEqualTo(receipts);
    }

    @Test
    void findWardPenalties_delegatesByStudentId() {
        PenaltyResponse response = new PenaltyResponse(45L, "Oviya Thangam", "GNM4-015", new BigDecimal("1500.00"), List.of());
        when(penaltyCalculationService.calculatePenalties(45L)).thenReturn(response);

        assertThat(service.findWardPenalties(45L)).contains(response);
    }

    @Test
    void findWardPenalties_emptyNotErrorWhenNoAllocationFinalizedYet() {
        when(penaltyCalculationService.calculatePenalties(45L))
            .thenThrow(new ResourceNotFoundException("Fee allocation not found for student: GNM4-015"));

        assertThat(service.findWardPenalties(45L)).isEmpty();
    }

    private StudentFeeAllocationResponse allocationResponse() {
        Instant now = Instant.now();
        return new StudentFeeAllocationResponse(
            1L, 45L, "Oviya Thangam", "GNM4-015", 1L, "GNM",
            new BigDecimal("200000.00"), new BigDecimal("10000.00"), null,
            BigDecimal.ZERO, new BigDecimal("190000.00"), "FINALIZED",
            now, "admin", List.of(), now, now
        );
    }

    private ReceiptResponse receiptResponse() {
        return new ReceiptResponse(
            1L, "RCP-2025-0001", 45L, "Oviya Thangam", "GNM4-015",
            10L, "Year 1", 1,
            new BigDecimal("50000.00"), LocalDate.of(2025, 1, 15), "UPI",
            "TXN-UPI-12345", "Installment payment", Instant.now(),
            "PAYMENT", null, null
        );
    }
}
