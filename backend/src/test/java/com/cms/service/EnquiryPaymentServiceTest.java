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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.EnquiryPaymentRequest;
import com.cms.dto.EnquiryPaymentResponse;
import com.cms.dto.EnquiryYearWiseFeeStatusResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Enquiry;
import com.cms.model.EnquiryPayment;
import com.cms.model.ReferralType;
import com.cms.model.enums.EnquiryStatus;
import com.cms.model.enums.PaymentMode;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.EnquiryStatusHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class EnquiryPaymentServiceTest {

    @Mock
    private EnquiryPaymentRepository enquiryPaymentRepository;

    @Mock
    private EnquiryRepository enquiryRepository;

    @Mock
    private EnquiryStatusHistoryRepository statusHistoryRepository;

    private EnquiryPaymentService enquiryPaymentService;

    private Enquiry testEnquiry;

    @BeforeEach
    void setUp() {
        enquiryPaymentService = new EnquiryPaymentService(
            enquiryPaymentRepository, enquiryRepository, statusHistoryRepository,
            new ObjectMapper()
        );

        testEnquiry = new Enquiry("Ravi Kumar", "ravi@email.com", "9876543210", null,
            LocalDate.of(2024, 6, 15),
            new ReferralType("Walk In", "WALK_IN", BigDecimal.ZERO, false, "Walk in", true),
            EnquiryStatus.FEES_FINALIZED);
        testEnquiry.setId(1L);
        testEnquiry.setFinalizedNetFee(new BigDecimal("100000.00"));
        testEnquiry.setCreatedAt(Instant.now());
        testEnquiry.setUpdatedAt(Instant.now());
    }

    @Test
    void shouldCollectFullPayment() {
        EnquiryPaymentRequest request = new EnquiryPaymentRequest(
            new BigDecimal("100000.00"),
            LocalDate.of(2024, 7, 1),
            PaymentMode.CASH,
            null,
            null
        );

        EnquiryPayment savedPayment = createPayment(1L, testEnquiry, new BigDecimal("100000.00"), "RCP-20240701-ABCD1234");
        savedPayment.setCreatedAt(Instant.now());

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(testEnquiry));
        when(enquiryPaymentRepository.save(any(EnquiryPayment.class))).thenReturn(savedPayment);
        when(enquiryPaymentRepository.sumAmountPaidByEnquiryId(1L)).thenReturn(new BigDecimal("100000.00"));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(testEnquiry);

        EnquiryPaymentResponse response = enquiryPaymentService.collectPayment(1L, request, "cashier");

        assertThat(response.amountPaid()).isEqualTo(new BigDecimal("100000.00"));
        assertThat(response.newStatus()).isEqualTo(EnquiryStatus.FEES_PAID.name());
        verify(enquiryRepository).save(any(Enquiry.class));
        verify(statusHistoryRepository).save(any());

        ArgumentCaptor<Enquiry> enquiryCaptor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository).save(enquiryCaptor.capture());
        assertThat(enquiryCaptor.getValue().getStatus()).isEqualTo(EnquiryStatus.FEES_PAID);
    }

    @Test
    void shouldCollectPartialPayment() {
        EnquiryPaymentRequest request = new EnquiryPaymentRequest(
            new BigDecimal("50000.00"),
            LocalDate.of(2024, 7, 1),
            PaymentMode.UPI,
            "TXN123",
            "First installment"
        );

        EnquiryPayment savedPayment = createPayment(1L, testEnquiry, new BigDecimal("50000.00"), "RCP-20240701-EFGH5678");
        savedPayment.setCreatedAt(Instant.now());

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(testEnquiry));
        when(enquiryPaymentRepository.save(any(EnquiryPayment.class))).thenReturn(savedPayment);
        when(enquiryPaymentRepository.sumAmountPaidByEnquiryId(1L)).thenReturn(new BigDecimal("50000.00"));
        when(enquiryRepository.save(any(Enquiry.class))).thenReturn(testEnquiry);

        EnquiryPaymentResponse response = enquiryPaymentService.collectPayment(1L, request, "cashier");

        assertThat(response.newStatus()).isEqualTo(EnquiryStatus.PARTIALLY_PAID.name());

        ArgumentCaptor<Enquiry> enquiryCaptor = ArgumentCaptor.forClass(Enquiry.class);
        verify(enquiryRepository).save(enquiryCaptor.capture());
        assertThat(enquiryCaptor.getValue().getStatus()).isEqualTo(EnquiryStatus.PARTIALLY_PAID);
    }

    @Test
    void shouldRejectPaymentWhenStatusNotEligible() {
        testEnquiry.setStatus(EnquiryStatus.ENQUIRED);

        EnquiryPaymentRequest request = new EnquiryPaymentRequest(
            new BigDecimal("50000.00"),
            LocalDate.of(2024, 7, 1),
            PaymentMode.CASH,
            null,
            null
        );

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(testEnquiry));

        assertThatThrownBy(() -> enquiryPaymentService.collectPayment(1L, request, "cashier"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("FEES_FINALIZED or PARTIALLY_PAID");
    }

    @Test
    void shouldGetPaymentsByEnquiryId() {
        EnquiryPayment payment = createPayment(1L, testEnquiry, new BigDecimal("50000.00"), "RCP-20240701-ABCD1234");
        payment.setCreatedAt(Instant.now());

        when(enquiryRepository.existsById(1L)).thenReturn(true);
        when(enquiryPaymentRepository.findByEnquiryIdOrderByPaymentDateDesc(1L)).thenReturn(List.of(payment));

        List<EnquiryPaymentResponse> responses = enquiryPaymentService.getPaymentsByEnquiryId(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).amountPaid()).isEqualTo(new BigDecimal("50000.00"));
        verify(enquiryPaymentRepository).findByEnquiryIdOrderByPaymentDateDesc(1L);
    }

    @Test
    void shouldThrowWhenEnquiryNotFoundOnGetPayments() {
        when(enquiryRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> enquiryPaymentService.getPaymentsByEnquiryId(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");
    }

    @Test
    void shouldGetReceipt() {
        EnquiryPayment payment = createPayment(1L, testEnquiry, new BigDecimal("50000.00"), "RCP-20240701-ABCD1234");
        payment.setCreatedAt(Instant.now());

        when(enquiryPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        EnquiryPaymentResponse response = enquiryPaymentService.getReceipt(1L, 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.receiptNumber()).isEqualTo("RCP-20240701-ABCD1234");
    }

    @Test
    void shouldThrowWhenReceiptPaymentNotFoundOnGetReceipt() {
        when(enquiryPaymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryPaymentService.getReceipt(1L, 999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Payment not found with id: 999");
    }

    @Test
    void shouldThrowWhenReceiptBelongsToDifferentEnquiry() {
        Enquiry otherEnquiry = new Enquiry("Other", "other@email.com", "0000000000", null,
            LocalDate.of(2024, 6, 15),
            new ReferralType("Walk In", "WALK_IN", BigDecimal.ZERO, false, "Walk in", true),
            EnquiryStatus.FEES_FINALIZED);
        otherEnquiry.setId(2L);

        EnquiryPayment payment = createPayment(1L, otherEnquiry, new BigDecimal("50000.00"), "RCP-20240701-ABCD1234");
        payment.setCreatedAt(Instant.now());

        when(enquiryPaymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> enquiryPaymentService.getReceipt(1L, 1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("does not belong to enquiry");
    }

    @Test
    void shouldReturnYearWiseFeeStatusWithWaterfallAllocation() {
        testEnquiry.setYearWiseFees("[{\"yearNumber\":1,\"amount\":30000},{\"yearNumber\":2,\"amount\":40000},{\"yearNumber\":3,\"amount\":30000}]");

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(testEnquiry));
        when(enquiryPaymentRepository.sumAmountPaidByEnquiryId(1L)).thenReturn(new BigDecimal("50000.00"));

        EnquiryYearWiseFeeStatusResponse response = enquiryPaymentService.getYearWiseFeeStatus(1L);

        assertThat(response.enquiryId()).isEqualTo(1L);
        assertThat(response.totalFee()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(response.totalPaid()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(response.totalOutstanding()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(response.yearBreakdown()).hasSize(3);

        // Year 1: 30000 allocated, 30000 paid (fully covered)
        assertThat(response.yearBreakdown().get(0).yearNumber()).isEqualTo(1);
        assertThat(response.yearBreakdown().get(0).paidAmount()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(response.yearBreakdown().get(0).outstanding()).isEqualByComparingTo(BigDecimal.ZERO);

        // Year 2: 40000 allocated, 20000 paid (partially covered)
        assertThat(response.yearBreakdown().get(1).yearNumber()).isEqualTo(2);
        assertThat(response.yearBreakdown().get(1).paidAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(response.yearBreakdown().get(1).outstanding()).isEqualByComparingTo(new BigDecimal("20000"));

        // Year 3: 30000 allocated, 0 paid (not covered yet)
        assertThat(response.yearBreakdown().get(2).yearNumber()).isEqualTo(3);
        assertThat(response.yearBreakdown().get(2).paidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.yearBreakdown().get(2).outstanding()).isEqualByComparingTo(new BigDecimal("30000"));
    }

    @Test
    void shouldReturnYearWiseFeeStatusWithNoYearData() {
        testEnquiry.setYearWiseFees(null);

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(testEnquiry));
        when(enquiryPaymentRepository.sumAmountPaidByEnquiryId(1L)).thenReturn(BigDecimal.ZERO);

        EnquiryYearWiseFeeStatusResponse response = enquiryPaymentService.getYearWiseFeeStatus(1L);

        assertThat(response.yearBreakdown()).isEmpty();
        assertThat(response.totalFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalOutstanding()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnYearWiseFeeStatusFullyPaid() {
        testEnquiry.setYearWiseFees("[{\"yearNumber\":1,\"amount\":50000},{\"yearNumber\":2,\"amount\":50000}]");

        when(enquiryRepository.findById(1L)).thenReturn(Optional.of(testEnquiry));
        when(enquiryPaymentRepository.sumAmountPaidByEnquiryId(1L)).thenReturn(new BigDecimal("100000.00"));

        EnquiryYearWiseFeeStatusResponse response = enquiryPaymentService.getYearWiseFeeStatus(1L);

        assertThat(response.totalOutstanding()).isEqualByComparingTo(BigDecimal.ZERO);
        response.yearBreakdown().forEach(y -> assertThat(y.outstanding()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    void shouldThrowWhenEnquiryNotFoundOnYearWiseFeeStatus() {
        when(enquiryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enquiryPaymentService.getYearWiseFeeStatus(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Enquiry not found with id: 999");
    }

    @Test
    void getTotalAmountPaid_returnsSum() {
        when(enquiryPaymentRepository.sumAmountPaidByEnquiryId(1L)).thenReturn(new BigDecimal("75000.00"));

        BigDecimal result = enquiryPaymentService.getTotalAmountPaid(1L);

        assertThat(result).isEqualByComparingTo("75000.00");
    }

    @Test
    void getTotalAmountPaid_returnsZeroWhenNoPayments() {
        when(enquiryPaymentRepository.sumAmountPaidByEnquiryId(1L)).thenReturn(null);

        BigDecimal result = enquiryPaymentService.getTotalAmountPaid(1L);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private EnquiryPayment createPayment(Long id, Enquiry enquiry, BigDecimal amount, String receiptNumber) {
        EnquiryPayment payment = new EnquiryPayment(
            enquiry, amount, LocalDate.of(2024, 7, 1), PaymentMode.CASH, null, null, receiptNumber, "cashier"
        );
        payment.setId(id);
        return payment;
    }
}
