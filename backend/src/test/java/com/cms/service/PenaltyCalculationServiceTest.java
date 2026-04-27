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

import com.cms.dto.PenaltyResponse;
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
class PenaltyCalculationServiceTest {

    @Mock
    private PenaltyRepository penaltyRepository;
    @Mock
    private SemesterFeeRepository semesterFeeRepository;
    @Mock
    private FeeInstallmentRepository installmentRepository;
    @Mock
    private StudentFeeAllocationRepository allocationRepository;
    @Mock
    private StudentRepository studentRepository;

    private PenaltyCalculationService service;

    private Student testStudent;
    private Program testProgram;
    private StudentFeeAllocation testAllocation;
    private SemesterFee semesterFee1;

    @BeforeEach
    void setUp() {
        service = new PenaltyCalculationService(penaltyRepository, semesterFeeRepository,
            installmentRepository, allocationRepository, studentRepository);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Sc CS");

        testStudent = new Student("CS2024001", "John", "Doe", "john@college.edu",
            testProgram, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        testStudent.setId(1L);

        testAllocation = new StudentFeeAllocation(
            testStudent, testProgram, new BigDecimal("200000"),
            BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("200000"),
            FeeAllocationStatus.FINALIZED
        );
        testAllocation.setId(1L);

        semesterFee1 = new SemesterFee(testAllocation, 1, "Year 1", new BigDecimal("200000"),
            LocalDate.now().minusDays(30)); // Due 30 days ago
        semesterFee1.setId(1L);
    }

    @Test
    void shouldCalculatePenaltyForOverdueSemester() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(testAllocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L)).thenReturn(List.of(semesterFee1));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(BigDecimal.ZERO);
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of());
        when(penaltyRepository.save(any(Penalty.class))).thenAnswer(inv -> {
            Penalty p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(penaltyRepository.findByStudentId(1L)).thenAnswer(inv -> {
            Penalty p = new Penalty(semesterFee1, testStudent, new BigDecimal("100"),
                semesterFee1.getDueDate(), new BigDecimal("3000"));
            p.setId(1L);
            p.setPenaltyEndDate(LocalDate.now());
            return List.of(p);
        });

        PenaltyResponse response = service.calculatePenalties(1L);

        assertThat(response.studentId()).isEqualTo(1L);
        assertThat(response.totalPenalty()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.penalties()).isNotEmpty();
    }

    @Test
    void shouldNotCreatePenaltyWhenFullyPaid() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(testAllocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L)).thenReturn(List.of(semesterFee1));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(new BigDecimal("200000"));
        when(penaltyRepository.findByStudentId(1L)).thenReturn(List.of());

        PenaltyResponse response = service.calculatePenalties(1L);

        assertThat(response.totalPenalty()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.penalties()).isEmpty();
    }

    @Test
    void shouldUpdateExistingPenalty() {
        Penalty existingPenalty = new Penalty(semesterFee1, testStudent, new BigDecimal("100"),
            semesterFee1.getDueDate(), new BigDecimal("1000"));
        existingPenalty.setId(1L);
        existingPenalty.setPenaltyEndDate(LocalDate.now().minusDays(10));

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(testAllocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L)).thenReturn(List.of(semesterFee1));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(BigDecimal.ZERO);
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of(existingPenalty));
        when(penaltyRepository.save(any(Penalty.class))).thenAnswer(inv -> inv.getArgument(0));
        when(penaltyRepository.findByStudentId(1L)).thenReturn(List.of(existingPenalty));

        PenaltyResponse response = service.calculatePenalties(1L);

        assertThat(response.penalties()).hasSize(1);
    }

    @Test
    void shouldNotUpdatePaidPenalty() {
        Penalty paidPenalty = new Penalty(semesterFee1, testStudent, new BigDecimal("100"),
            semesterFee1.getDueDate(), new BigDecimal("1000"));
        paidPenalty.setId(1L);
        paidPenalty.setIsPaid(true);
        paidPenalty.setPenaltyEndDate(LocalDate.now().minusDays(10));

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(testAllocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L)).thenReturn(List.of(semesterFee1));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(BigDecimal.ZERO);
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of(paidPenalty));
        when(penaltyRepository.findByStudentId(1L)).thenReturn(List.of(paidPenalty));

        PenaltyResponse response = service.calculatePenalties(1L);

        // Paid penalty should not count toward total
        assertThat(response.totalPenalty()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldNotCreatePenaltyForFutureDueDate() {
        SemesterFee futureFee = new SemesterFee(testAllocation, 2, "Year 2", new BigDecimal("200000"),
            LocalDate.now().plusDays(90));
        futureFee.setId(2L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(testAllocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L)).thenReturn(List.of(futureFee));
        when(penaltyRepository.findByStudentId(1L)).thenReturn(List.of());

        PenaltyResponse response = service.calculatePenalties(1L);

        assertThat(response.penalties()).isEmpty();
    }

    @Test
    void shouldThrowWhenStudentNotFound() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculatePenalties(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowWhenAllocationNotFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculatePenalties(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldGetPenalties() {
        Penalty penalty = new Penalty(semesterFee1, testStudent, new BigDecimal("100"),
            LocalDate.now().minusDays(30), new BigDecimal("3000"));
        penalty.setId(1L);
        penalty.setPenaltyEndDate(LocalDate.now());

        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(penaltyRepository.findByStudentId(1L)).thenReturn(List.of(penalty));

        PenaltyResponse response = service.getPenalties(1L);

        assertThat(response.totalPenalty()).isEqualTo(new BigDecimal("3000"));
        assertThat(response.penalties()).hasSize(1);
    }

    @Test
    void shouldThrowWhenStudentNotFoundForGetPenalties() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPenalties(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
