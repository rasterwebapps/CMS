package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.cms.dto.FeeExplorerResponse;
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
class FeeExplorerServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentFeeAllocationRepository allocationRepository;
    @Mock
    private SemesterFeeRepository semesterFeeRepository;
    @Mock
    private FeeInstallmentRepository installmentRepository;
    @Mock
    private PenaltyRepository penaltyRepository;

    private FeeExplorerService service;

    private Student testStudent;
    private Program testProgram;

    @BeforeEach
    void setUp() {
        service = new FeeExplorerService(studentRepository, allocationRepository,
            semesterFeeRepository, installmentRepository, penaltyRepository);

        testProgram = new Program();
        testProgram.setId(1L);
        testProgram.setName("B.Sc CS");

        testStudent = new Student("CS2024001", "John", "Doe", "john@college.edu",
            testProgram, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        testStudent.setId(1L);
    }

    @Test
    void shouldSearchByRollNumber() {
        StudentFeeAllocation allocation = new StudentFeeAllocation(
            testStudent, testProgram, new BigDecimal("200000"),
            BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("200000"),
            FeeAllocationStatus.FINALIZED
        );
        allocation.setId(1L);

        SemesterFee sf = new SemesterFee(allocation, 1, "Year 1", new BigDecimal("200000"), LocalDate.of(2024, 7, 31));
        sf.setId(1L);

        when(studentRepository.findByRollNumber("CS2024001")).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumber(1L)).thenReturn(List.of(sf));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(new BigDecimal("50000"));
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of());

        FeeExplorerResponse response = service.search("CS2024001");

        assertThat(response.students()).hasSize(1);
        assertThat(response.students().get(0).rollNumber()).isEqualTo("CS2024001");
        assertThat(response.students().get(0).totalPaid()).isEqualTo(new BigDecimal("50000"));
    }

    @Test
    void shouldSearchByProgramId() {
        when(studentRepository.findByRollNumber("1")).thenReturn(Optional.empty());
        when(studentRepository.findByProgramId(1L)).thenReturn(List.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.empty());

        FeeExplorerResponse response = service.search("1");

        assertThat(response.students()).hasSize(1);
        assertThat(response.students().get(0).allocationStatus()).isEqualTo("NOT_ALLOCATED");
    }

    @Test
    void shouldSearchByPartialRollNumber() {
        when(studentRepository.findByRollNumber("CS2024")).thenReturn(Optional.empty());
        when(studentRepository.findByRollNumberContainingIgnoreCase("CS2024")).thenReturn(List.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.empty());

        FeeExplorerResponse response = service.search("CS2024");

        assertThat(response.students()).hasSize(1);
    }

    @Test
    void shouldReturnAllStudentsWhenSearchIsNull() {
        when(studentRepository.findAll()).thenReturn(List.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.empty());

        FeeExplorerResponse response = service.search(null);

        assertThat(response.students()).hasSize(1);
    }

    @Test
    void shouldReturnAllStudentsWhenSearchIsBlank() {
        when(studentRepository.findAll()).thenReturn(List.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.empty());

        FeeExplorerResponse response = service.search("  ");

        assertThat(response.students()).hasSize(1);
    }

    @Test
    void shouldIncludePenaltyInSummary() {
        StudentFeeAllocation allocation = new StudentFeeAllocation(
            testStudent, testProgram, new BigDecimal("200000"),
            BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("200000"),
            FeeAllocationStatus.FINALIZED
        );
        allocation.setId(1L);

        SemesterFee sf = new SemesterFee(allocation, 1, "Year 1", new BigDecimal("200000"), LocalDate.of(2024, 7, 31));
        sf.setId(1L);

        Penalty penalty = new Penalty(sf, testStudent, new BigDecimal("100"),
            LocalDate.now().minusDays(30), new BigDecimal("3000"));
        penalty.setId(1L);

        when(studentRepository.findByRollNumber("CS2024001")).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumber(1L)).thenReturn(List.of(sf));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(BigDecimal.ZERO);
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of(penalty));

        FeeExplorerResponse response = service.search("CS2024001");

        assertThat(response.students().get(0).totalPenalty()).isEqualTo(new BigDecimal("3000"));
    }

    @Test
    void shouldReturnEmptyForNoResults() {
        when(studentRepository.findByRollNumber("NONEXISTENT")).thenReturn(Optional.empty());
        when(studentRepository.findByRollNumberContainingIgnoreCase("NONEXISTENT")).thenReturn(List.of());

        FeeExplorerResponse response = service.search("NONEXISTENT");

        assertThat(response.students()).isEmpty();
    }
}
