package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.DisbursementRequest;
import com.cms.model.AcademicYear;
import com.cms.model.Program;
import com.cms.model.ScholarshipDisbursement;
import com.cms.model.ScholarshipType;
import com.cms.model.Student;
import com.cms.model.StudentScholarship;
import com.cms.model.enums.DisbursementMode;
import com.cms.model.enums.ScholarshipStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.ScholarshipDisbursementRepository;
import com.cms.repository.StudentScholarshipRepository;

@ExtendWith(MockitoExtension.class)
class ScholarshipDisbursementServiceTest {
    @Mock private ScholarshipDisbursementRepository disbursementRepository;
    @Mock private StudentScholarshipRepository applicationRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    private ScholarshipDisbursementService service;
    private StudentScholarship application;

    @BeforeEach
    void setUp() {
        service = new ScholarshipDisbursementService(disbursementRepository, applicationRepository, academicYearRepository);
        Program program = new Program("Bachelor", "BACH", 4);
        Student student = new Student("S1", "A", "B", "a@test.com", program, 1, LocalDate.now(), StudentStatus.ACTIVE);
        student.setId(1L);
        ScholarshipType type = new ScholarshipType(); type.setId(2L); type.setName("SC"); type.setCode("SC");
        AcademicYear ay = new AcademicYear("2026-2027", LocalDate.now(), LocalDate.now().plusYears(1), true); ay.setId(10L);
        application = new StudentScholarship(); application.setId(3L); application.setStudent(student); application.setScholarshipType(type); application.setAcademicYear(ay); application.setStatus(ScholarshipStatus.APPROVED);
    }

    @Test
    void shouldDisburseApprovedApplicationAndListHistory() {
        when(applicationRepository.findById(3L)).thenReturn(Optional.of(application));
        when(disbursementRepository.save(any(ScholarshipDisbursement.class))).thenAnswer(inv -> { ScholarshipDisbursement d = inv.getArgument(0); d.setId(4L); return d; });
        var request = new DisbursementRequest(null, 1, new BigDecimal("5000"), LocalDate.now(), DisbursementMode.FEE_WAIVER, "TXN", null, null, "ok");
        var response = service.disburse(3L, request, "cashier");
        assertThat(response.amount()).isEqualByComparingTo("5000");
        assertThat(response.disbursedBy()).isEqualTo("cashier");

        ScholarshipDisbursement d = new ScholarshipDisbursement(); d.setId(4L); d.setStudentScholarship(application); d.setAcademicYear(application.getAcademicYear()); d.setAmount(new BigDecimal("5000")); d.setDisbursementDate(LocalDate.now()); d.setDisbursementMode(DisbursementMode.FEE_WAIVER);
        when(applicationRepository.existsById(3L)).thenReturn(true);
        when(disbursementRepository.findByStudentScholarshipIdOrderByDisbursementDateDesc(3L)).thenReturn(List.of(d));
        when(disbursementRepository.findByStudentScholarshipStudentIdOrderByDisbursementDateDesc(1L)).thenReturn(List.of(d));
        assertThat(service.getApplicationDisbursements(3L)).hasSize(1);
        assertThat(service.getStudentDisbursementHistory(1L)).hasSize(1);
    }

    @Test
    void shouldRejectDisbursementForPendingApplication() {
        application.setStatus(ScholarshipStatus.PENDING);
        when(applicationRepository.findById(3L)).thenReturn(Optional.of(application));
        var request = new DisbursementRequest(null, null, BigDecimal.ONE, LocalDate.now(), DisbursementMode.CHEQUE, null, null, null, null);
        assertThatThrownBy(() -> service.disburse(3L, request, "cashier")).isInstanceOf(IllegalStateException.class);
    }
}

