package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
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

import com.cms.dto.PenaltyResponse;
import com.cms.dto.ReceiptResponse;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AppUser;
import com.cms.model.Student;
import com.cms.repository.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class StudentFeeSelfServiceServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private FeeFinalizationService feeFinalizationService;

    @Mock
    private PaymentCollectionService paymentCollectionService;

    @Mock
    private PenaltyCalculationService penaltyCalculationService;

    private StudentFeeSelfServiceService service;

    private AppUser linkedUser;
    private AppUser unlinkedUser;

    @BeforeEach
    void setUp() {
        service = new StudentFeeSelfServiceService(appUserRepository, feeFinalizationService,
            paymentCollectionService, penaltyCalculationService);

        Student student = new Student();
        student.setId(45L);

        linkedUser = new AppUser();
        linkedUser.setLinkedStudent(student);

        unlinkedUser = new AppUser();
    }

    @Test
    void findMySummary_resolvesCallerLinkedStudentAndReusesExistingLookup() {
        StudentFeeAllocationResponse response = allocationResponse();
        when(appUserRepository.findByKeycloakUsername("teststudent45")).thenReturn(Optional.of(linkedUser));
        when(feeFinalizationService.getByStudentId(45L)).thenReturn(response);

        Optional<StudentFeeAllocationResponse> result = service.findMySummary("teststudent45");

        assertThat(result).contains(response);
    }

    @Test
    void findMySummary_emptyWhenCallerHasNoLinkedStudent() {
        when(appUserRepository.findByKeycloakUsername("devadmin")).thenReturn(Optional.of(unlinkedUser));

        Optional<StudentFeeAllocationResponse> result = service.findMySummary("devadmin");

        assertThat(result).isEmpty();
        verify(feeFinalizationService, never()).getByStudentId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void findMySummary_emptyWhenCallerHasNoAppUserRow() {
        when(appUserRepository.findByKeycloakUsername("ghost")).thenReturn(Optional.empty());

        assertThat(service.findMySummary("ghost")).isEmpty();
    }

    @Test
    void findMySummary_emptyNotErrorWhenNoFeeAllocationFinalizedYet() {
        when(appUserRepository.findByKeycloakUsername("teststudent45")).thenReturn(Optional.of(linkedUser));
        when(feeFinalizationService.getByStudentId(45L))
            .thenThrow(new ResourceNotFoundException("Fee allocation not found for student id: 45"));

        assertThat(service.findMySummary("teststudent45")).isEmpty();
    }

    @Test
    void findMyReceipts_resolvesCallerLinkedStudent() {
        List<ReceiptResponse> receipts = List.of(receiptResponse());
        when(appUserRepository.findByKeycloakUsername("teststudent45")).thenReturn(Optional.of(linkedUser));
        when(paymentCollectionService.getReceipts(45L)).thenReturn(receipts);

        assertThat(service.findMyReceipts("teststudent45")).isEqualTo(receipts);
    }

    @Test
    void findMyReceipts_emptyWhenCallerHasNoLinkedStudent() {
        when(appUserRepository.findByKeycloakUsername("devadmin")).thenReturn(Optional.of(unlinkedUser));

        assertThat(service.findMyReceipts("devadmin")).isEmpty();
        verify(paymentCollectionService, never()).getReceipts(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void findMyPenalties_resolvesCallerLinkedStudent() {
        PenaltyResponse response = penaltyResponse();
        when(appUserRepository.findByKeycloakUsername("teststudent45")).thenReturn(Optional.of(linkedUser));
        when(penaltyCalculationService.calculatePenalties(45L)).thenReturn(response);

        assertThat(service.findMyPenalties("teststudent45")).contains(response);
    }

    @Test
    void findMyPenalties_emptyNotErrorWhenNoFeeAllocationFinalizedYet() {
        when(appUserRepository.findByKeycloakUsername("teststudent45")).thenReturn(Optional.of(linkedUser));
        when(penaltyCalculationService.calculatePenalties(45L))
            .thenThrow(new ResourceNotFoundException("Fee allocation not found for student: GNM4-015"));

        assertThat(service.findMyPenalties("teststudent45")).isEmpty();
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

    private PenaltyResponse penaltyResponse() {
        return new PenaltyResponse(45L, "Oviya Thangam", "GNM4-015", new BigDecimal("1500.00"), List.of());
    }
}
