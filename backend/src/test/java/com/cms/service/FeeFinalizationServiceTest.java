package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.StudentFeeAllocationRequest;
import com.cms.dto.StudentFeeAllocationResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.Penalty;
import com.cms.model.Program;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.PenaltyRepository;
import com.cms.repository.SemesterFeeRepository;
import com.cms.repository.StudentFeeAllocationRepository;
import com.cms.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeeFinalizationServiceTest {

    @Mock private StudentFeeAllocationRepository allocationRepository;
    @Mock private SemesterFeeRepository semesterFeeRepository;
    @Mock private FeeInstallmentRepository installmentRepository;
    @Mock private PenaltyRepository penaltyRepository;
    @Mock private StudentRepository studentRepository;

    private FeeFinalizationService service;
    private Student testStudent;
    private Program testProgram;

    @BeforeEach
    void setUp() {
        service = new FeeFinalizationService(allocationRepository, semesterFeeRepository,
            installmentRepository, penaltyRepository, studentRepository);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Sc CS");

        testStudent = new Student("CS2024001", "John", "Doe", "john@college.edu",
            testProgram, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        testStudent.setId(1L);
    }

    @Test
    void shouldFinalizeStudentFee() {
        List<StudentFeeAllocationRequest.YearFee> yearFees = List.of(
            new StudentFeeAllocationRequest.YearFee(1, new BigDecimal("235000"), LocalDate.of(2024, 7, 31)),
            new StudentFeeAllocationRequest.YearFee(2, new BigDecimal("200000"), LocalDate.of(2025, 7, 31)),
            new StudentFeeAllocationRequest.YearFee(3, new BigDecimal("200000"), LocalDate.of(2026, 7, 31)),
            new StudentFeeAllocationRequest.YearFee(4, new BigDecimal("200000"), LocalDate.of(2027, 7, 31))
        );

        StudentFeeAllocationRequest request = new StudentFeeAllocationRequest(
            1L, new BigDecimal("835000"), BigDecimal.ZERO, null, BigDecimal.ZERO, yearFees
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.existsByStudentId(1L)).thenReturn(false);
        when(allocationRepository.save(any(StudentFeeAllocation.class))).thenAnswer(inv -> {
            StudentFeeAllocation a = inv.getArgument(0);
            a.setId(1L);
            a.setCreatedAt(Instant.now());
            a.setUpdatedAt(Instant.now());
            return a;
        });

        // Use AtomicLong counter to avoid ID collision (two semesters per year)
        AtomicLong idCounter = new AtomicLong(1);
        when(semesterFeeRepository.save(any(SemesterFee.class))).thenAnswer(inv -> {
            SemesterFee sf = inv.getArgument(0);
            sf.setId(idCounter.getAndIncrement());
            return sf;
        });
        when(installmentRepository.sumAmountPaidBySemesterFeeId(any())).thenReturn(BigDecimal.ZERO);
        when(penaltyRepository.findBySemesterFeeId(any())).thenReturn(List.of());

        StudentFeeAllocationResponse response = service.finalize(request, "admin");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.netFee()).isEqualTo(new BigDecimal("835000"));
        assertThat(response.status()).isEqualTo("FINALIZED");
        // 4 years × 2 semesters = 8 semester records
        assertThat(response.semesterFees()).hasSize(8);

        // Year 1 (₹235000) → S1 = floor(235000/2) = ₹117500.00, S2 = ₹117500.00
        assertThat(response.semesterFees().get(0).semesterLabel()).isEqualTo("Year 1 - Semester 1");
        assertThat(response.semesterFees().get(0).semesterSequence()).isEqualTo(1);
        assertThat(response.semesterFees().get(0).amount()).isEqualByComparingTo("117500.00");

        assertThat(response.semesterFees().get(1).semesterLabel()).isEqualTo("Year 1 - Semester 2");
        assertThat(response.semesterFees().get(1).semesterSequence()).isEqualTo(2);
        assertThat(response.semesterFees().get(1).amount()).isEqualByComparingTo("117500.00");
    }

    @Test
    void shouldSplitOddAmountCorrectly() {
        List<StudentFeeAllocationRequest.YearFee> yearFees = List.of(
            new StudentFeeAllocationRequest.YearFee(1, new BigDecimal("100001"), LocalDate.of(2024, 7, 31))
        );
        StudentFeeAllocationRequest request = new StudentFeeAllocationRequest(
            1L, new BigDecimal("100001"), null, null, null, yearFees
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.existsByStudentId(1L)).thenReturn(false);
        when(allocationRepository.save(any())).thenAnswer(inv -> {
            StudentFeeAllocation a = inv.getArgument(0);
            a.setId(1L); a.setCreatedAt(Instant.now()); a.setUpdatedAt(Instant.now());
            return a;
        });
        AtomicLong idCounter = new AtomicLong(1);
        when(semesterFeeRepository.save(any(SemesterFee.class))).thenAnswer(inv -> {
            SemesterFee sf = inv.getArgument(0); sf.setId(idCounter.getAndIncrement()); return sf;
        });
        when(installmentRepository.sumAmountPaidBySemesterFeeId(any())).thenReturn(BigDecimal.ZERO);
        when(penaltyRepository.findBySemesterFeeId(any())).thenReturn(List.of());

        StudentFeeAllocationResponse response = service.finalize(request, "admin");

        BigDecimal s1 = response.semesterFees().get(0).amount();
        BigDecimal s2 = response.semesterFees().get(1).amount();
        assertThat(s1.add(s2)).isEqualByComparingTo("100001");
        assertThat(s2).isGreaterThanOrEqualTo(s1); // remainder goes to S2
    }

    @Test
    void shouldSetSemester2DueDateSixMonthsAfterSemester1() {
        LocalDate dueDate = LocalDate.of(2024, 7, 31);
        List<StudentFeeAllocationRequest.YearFee> yearFees = List.of(
            new StudentFeeAllocationRequest.YearFee(1, new BigDecimal("200000"), dueDate)
        );
        StudentFeeAllocationRequest request = new StudentFeeAllocationRequest(
            1L, new BigDecimal("200000"), null, null, null, yearFees
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.existsByStudentId(1L)).thenReturn(false);
        when(allocationRepository.save(any())).thenAnswer(inv -> {
            StudentFeeAllocation a = inv.getArgument(0);
            a.setId(1L); a.setCreatedAt(Instant.now()); a.setUpdatedAt(Instant.now());
            return a;
        });
        AtomicLong idCounter = new AtomicLong(1);
        when(semesterFeeRepository.save(any(SemesterFee.class))).thenAnswer(inv -> {
            SemesterFee sf = inv.getArgument(0); sf.setId(idCounter.getAndIncrement()); return sf;
        });
        when(installmentRepository.sumAmountPaidBySemesterFeeId(any())).thenReturn(BigDecimal.ZERO);
        when(penaltyRepository.findBySemesterFeeId(any())).thenReturn(List.of());

        StudentFeeAllocationResponse response = service.finalize(request, "admin");

        assertThat(response.semesterFees().get(0).dueDate()).isEqualTo(dueDate);
        assertThat(response.semesterFees().get(1).dueDate()).isEqualTo(dueDate.plusMonths(6));
    }

    @Test
    void shouldThrowWhenStudentNotFound() {
        List<StudentFeeAllocationRequest.YearFee> yearFees = List.of(
            new StudentFeeAllocationRequest.YearFee(1, new BigDecimal("100000"), LocalDate.of(2024, 7, 31))
        );
        StudentFeeAllocationRequest request = new StudentFeeAllocationRequest(
            999L, new BigDecimal("100000"), null, null, null, yearFees
        );
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.finalize(request, "admin"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Student not found with id: 999");
    }

    @Test
    void shouldThrowWhenAllocationAlreadyExists() {
        List<StudentFeeAllocationRequest.YearFee> yearFees = List.of(
            new StudentFeeAllocationRequest.YearFee(1, new BigDecimal("100000"), LocalDate.of(2024, 7, 31))
        );
        StudentFeeAllocationRequest request = new StudentFeeAllocationRequest(
            1L, new BigDecimal("100000"), null, null, null, yearFees
        );
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.existsByStudentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.finalize(request, "admin"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Fee allocation already exists");
    }

    @Test
    void shouldFinalizeWithDiscount() {
        List<StudentFeeAllocationRequest.YearFee> yearFees = List.of(
            new StudentFeeAllocationRequest.YearFee(1, new BigDecimal("200000"), LocalDate.of(2024, 7, 31))
        );
        StudentFeeAllocationRequest request = new StudentFeeAllocationRequest(
            1L, new BigDecimal("250000"), new BigDecimal("50000"), "Merit scholarship",
            new BigDecimal("10000"), yearFees
        );

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.existsByStudentId(1L)).thenReturn(false);
        when(allocationRepository.save(any(StudentFeeAllocation.class))).thenAnswer(inv -> {
            StudentFeeAllocation a = inv.getArgument(0);
            a.setId(1L); a.setCreatedAt(Instant.now()); a.setUpdatedAt(Instant.now());
            return a;
        });
        AtomicLong idCounter = new AtomicLong(1);
        when(semesterFeeRepository.save(any(SemesterFee.class))).thenAnswer(inv -> {
            SemesterFee sf = inv.getArgument(0); sf.setId(idCounter.getAndIncrement()); return sf;
        });
        when(installmentRepository.sumAmountPaidBySemesterFeeId(any())).thenReturn(BigDecimal.ZERO);
        when(penaltyRepository.findBySemesterFeeId(any())).thenReturn(List.of());

        StudentFeeAllocationResponse response = service.finalize(request, "admin");

        assertThat(response.discountAmount()).isEqualTo(new BigDecimal("50000"));
        assertThat(response.netFee()).isEqualTo(new BigDecimal("200000"));
        assertThat(response.agentCommission()).isEqualTo(new BigDecimal("10000"));
        // 1 year → 2 semester records
        assertThat(response.semesterFees()).hasSize(2);
    }

    @Test
    void shouldGetByStudentId() {
        StudentFeeAllocation allocation = new StudentFeeAllocation(
            testStudent, testProgram, new BigDecimal("835000"),
            BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("835000"),
            FeeAllocationStatus.FINALIZED
        );
        allocation.setId(1L);
        allocation.setFinalizedAt(Instant.now());
        allocation.setFinalizedBy("admin");
        allocation.setCreatedAt(Instant.now());
        allocation.setUpdatedAt(Instant.now());

        SemesterFee sf1 = new SemesterFee(allocation, 1, "Year 1 - Semester 1",
            new BigDecimal("117500"), LocalDate.of(2024, 7, 31), 1);
        sf1.setId(1L);

        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L))
            .thenReturn(List.of(sf1));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(BigDecimal.ZERO);
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of());

        StudentFeeAllocationResponse response = service.getByStudentId(1L);

        assertThat(response.studentId()).isEqualTo(1L);
        assertThat(response.semesterFees()).hasSize(1);
        assertThat(response.semesterFees().get(0).semesterSequence()).isEqualTo(1);
    }

    @Test
    void shouldThrowWhenAllocationNotFoundByStudentId() {
        when(allocationRepository.findByStudentId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByStudentId(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldGetById() {
        StudentFeeAllocation allocation = new StudentFeeAllocation(
            testStudent, testProgram, new BigDecimal("835000"),
            BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("835000"),
            FeeAllocationStatus.FINALIZED
        );
        allocation.setId(1L);
        allocation.setFinalizedAt(Instant.now());
        allocation.setFinalizedBy("admin");
        allocation.setCreatedAt(Instant.now());
        allocation.setUpdatedAt(Instant.now());

        when(allocationRepository.findById(1L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L))
            .thenReturn(List.of());

        StudentFeeAllocationResponse response = service.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowWhenAllocationNotFoundById() {
        when(allocationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldShowPaidStatusInSemesterFees() {
        StudentFeeAllocation allocation = buildAllocation(new BigDecimal("200000"));
        SemesterFee sf1 = new SemesterFee(allocation, 1, "Year 1 - Semester 1",
            new BigDecimal("200000"), LocalDate.of(2024, 7, 31), 1);
        sf1.setId(1L);

        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L))
            .thenReturn(List.of(sf1));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(new BigDecimal("200000"));
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of());

        StudentFeeAllocationResponse response = service.getByStudentId(1L);

        assertThat(response.semesterFees().get(0).paymentStatus()).isEqualTo("PAID");
        assertThat(response.semesterFees().get(0).pendingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldShowPartialStatusInSemesterFees() {
        StudentFeeAllocation allocation = buildAllocation(new BigDecimal("200000"));
        SemesterFee sf1 = new SemesterFee(allocation, 1, "Year 1 - Semester 1",
            new BigDecimal("200000"), LocalDate.of(2024, 7, 31), 1);
        sf1.setId(1L);

        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L))
            .thenReturn(List.of(sf1));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(new BigDecimal("100000"));
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of());

        StudentFeeAllocationResponse response = service.getByStudentId(1L);

        assertThat(response.semesterFees().get(0).paymentStatus()).isEqualTo("PARTIAL");
        assertThat(response.semesterFees().get(0).pendingAmount()).isEqualByComparingTo("100000");
    }

    @Test
    void shouldShowPenaltyAmountInSemesterFees() {
        StudentFeeAllocation allocation = buildAllocation(new BigDecimal("200000"));
        SemesterFee sf1 = new SemesterFee(allocation, 1, "Year 1 - Semester 1",
            new BigDecimal("200000"), LocalDate.of(2024, 7, 31), 1);
        sf1.setId(1L);

        Penalty penalty = new Penalty(sf1, testStudent, new BigDecimal("100"),
            LocalDate.of(2024, 8, 1), new BigDecimal("3000"));
        penalty.setId(1L);

        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L))
            .thenReturn(List.of(sf1));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(BigDecimal.ZERO);
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of(penalty));

        StudentFeeAllocationResponse response = service.getByStudentId(1L);

        assertThat(response.semesterFees().get(0).penaltyAmount()).isEqualByComparingTo("3000");
    }

    private StudentFeeAllocation buildAllocation(BigDecimal amount) {
        StudentFeeAllocation a = new StudentFeeAllocation(
            testStudent, testProgram, amount, BigDecimal.ZERO, null, BigDecimal.ZERO, amount,
            FeeAllocationStatus.FINALIZED
        );
        a.setId(1L);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        return a;
    }
}
