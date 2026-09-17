package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.cms.dto.FeeExplorerResponse;
import com.cms.dto.FeeExplorerSemesterWiseRow;
import com.cms.model.AcademicYear;
import com.cms.model.Admission;
import com.cms.model.Enquiry;
import com.cms.model.Penalty;
import com.cms.model.Program;
import com.cms.model.SemesterFee;
import com.cms.model.Student;
import com.cms.model.StudentFeeAllocation;
import com.cms.model.enums.FeeAllocationStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.EnquiryCreditApplicationRepository;
import com.cms.repository.FeeInstallmentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.EnquiryPaymentRepository;
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
    @Mock
    private EnquiryRepository enquiryRepository;
    @Mock
    private EnquiryPaymentRepository enquiryPaymentRepository;
    @Mock
    private EnquiryCreditApplicationRepository creditApplicationRepository;
    @Mock
    private AdmissionRepository admissionRepository;
    @Mock
    private PaymentCollectionService paymentCollectionService;

    private FeeExplorerService service;

    private Student testStudent;
    private Program testProgram;

    @BeforeEach
    void setUp() {
        service = new FeeExplorerService(studentRepository, allocationRepository,
            semesterFeeRepository, installmentRepository, penaltyRepository,
            enquiryRepository, enquiryPaymentRepository, creditApplicationRepository,
            admissionRepository, paymentCollectionService);
        when(admissionRepository.findByStudentIdInFetchJoiningYear(any())).thenReturn(List.of());
        org.mockito.Mockito.lenient()
            .when(paymentCollectionService.getCollectibleOutstanding(any()))
            .thenReturn(BigDecimal.ZERO);

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
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L)).thenReturn(List.of(sf));
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
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L)).thenReturn(List.of(sf));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(BigDecimal.ZERO);
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of(penalty));

        FeeExplorerResponse response = service.search("CS2024001");

        assertThat(response.students().get(0).totalPenalty()).isEqualTo(new BigDecimal("3000"));
    }

    // ── Fix: enquiry pre-admission credit must count toward Paid, not just reduce Pending ──────

    @Test
    void shouldIncludeUnappliedEnquiryCreditInPaidAndPending() {
        StudentFeeAllocation allocation = new StudentFeeAllocation(
            testStudent, testProgram, new BigDecimal("300000"),
            BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("300000"),
            FeeAllocationStatus.FINALIZED
        );
        allocation.setId(1L);

        SemesterFee sf = new SemesterFee(allocation, 1, "Year 1", new BigDecimal("300000"), LocalDate.of(2024, 7, 31));
        sf.setId(1L);

        Enquiry enquiry = new Enquiry();
        enquiry.setId(8L);

        when(studentRepository.findByRollNumber("CS2024001")).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L)).thenReturn(List.of(sf));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(BigDecimal.ZERO);
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of());
        when(enquiryRepository.findByConvertedStudentId(1L)).thenReturn(Optional.of(enquiry));
        // Real pre-admission payment received, but not yet formally "applied" to any semester fee.
        when(enquiryPaymentRepository.sumAmountPaidByEnquiryId(8L)).thenReturn(new BigDecimal("50000"));
        when(creditApplicationRepository.sumAmountAppliedByEnquiryId(8L)).thenReturn(BigDecimal.ZERO);
        when(creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(8L, 1L)).thenReturn(BigDecimal.ZERO);

        FeeExplorerResponse response = service.search("CS2024001");

        FeeExplorerResponse.StudentFeeSummary summary = response.students().get(0);
        assertThat(summary.totalPaid()).isEqualByComparingTo("50000");
        assertThat(summary.totalPending()).isEqualByComparingTo("250000");
        // Paid + Pending must always reconcile to Total Fee.
        assertThat(summary.totalPaid().add(summary.totalPending())).isEqualByComparingTo(allocation.getNetFee());
    }

    @Test
    void shouldIncludeAlreadyAppliedEnquiryCreditInPaidWithoutDoubleCounting() {
        StudentFeeAllocation allocation = new StudentFeeAllocation(
            testStudent, testProgram, new BigDecimal("110000"),
            BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("110000"),
            FeeAllocationStatus.FINALIZED
        );
        allocation.setId(1L);

        SemesterFee sf1 = new SemesterFee(allocation, 1, "Year 1 - Sem 1", new BigDecimal("55000"), LocalDate.of(2024, 7, 31), 1);
        sf1.setId(1L);
        SemesterFee sf2 = new SemesterFee(allocation, 1, "Year 1 - Sem 2", new BigDecimal("55000"), LocalDate.of(2025, 1, 31), 2);
        sf2.setId(2L);

        Enquiry enquiry = new Enquiry();
        enquiry.setId(8L);

        when(studentRepository.findByRollNumber("CS2024001")).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L))
            .thenReturn(List.of(sf1, sf2));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(new BigDecimal("50000"));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(2L)).thenReturn(new BigDecimal("45000"));
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of());
        when(penaltyRepository.findBySemesterFeeId(2L)).thenReturn(List.of());
        when(enquiryRepository.findByConvertedStudentId(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryPaymentRepository.sumAmountPaidByEnquiryId(8L)).thenReturn(new BigDecimal("5000"));
        // The 5000 credit was already formally applied to semester 1 in a prior collection.
        when(creditApplicationRepository.sumAmountAppliedByEnquiryId(8L)).thenReturn(new BigDecimal("5000"));
        when(creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(8L, 1L)).thenReturn(new BigDecimal("5000"));
        when(creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(8L, 2L)).thenReturn(BigDecimal.ZERO);

        FeeExplorerResponse response = service.search("CS2024001");

        FeeExplorerResponse.StudentFeeSummary summary = response.students().get(0);
        // Semester 1 is fully settled (50000 installment + 5000 credit = 55000); semester 2 still owes 10000.
        assertThat(summary.totalPaid()).isEqualByComparingTo("100000");
        assertThat(summary.totalPending()).isEqualByComparingTo("10000");
        assertThat(summary.totalPaid().add(summary.totalPending())).isEqualByComparingTo(allocation.getNetFee());
    }

    @Test
    void shouldReturnEmptyForNoResults() {
        when(studentRepository.findByRollNumber("NONEXISTENT")).thenReturn(Optional.empty());
        when(studentRepository.findByRollNumberContainingIgnoreCase("NONEXISTENT")).thenReturn(List.of());

        FeeExplorerResponse response = service.search("NONEXISTENT");

        assertThat(response.students()).isEmpty();
    }

    // ── Bug fix: dropdown filters (program/academicYear/yearOfStudy/allocationStatus) must apply
    // across every student, not just whichever page happens to already be loaded ─────────────────

    @Test
    void shouldUseDbPaginationFastPathWhenNoDropdownFilterActive() {
        Pageable pageable = PageRequest.of(0, 25);
        Page<Student> idPage = new PageImpl<>(List.of(testStudent), pageable, 1);

        when(studentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
            .thenReturn(idPage);
        when(studentRepository.findByIdInWithRelations(List.of(1L))).thenReturn(List.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.empty());

        Page<FeeExplorerResponse.StudentFeeSummary> page =
            service.searchPageable(null, null, null, null, null, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).rollNumber()).isEqualTo("CS2024001");
    }

    @Test
    void shouldFilterByProgramAcrossFullDatasetNotJustOnePage() {
        Program itProgram = new Program();
        itProgram.setId(2L);
        itProgram.setName("B.Sc IT");
        Student otherStudent = new Student("IT2024001", "Jane", "Roe", "jane@college.edu",
            itProgram, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        otherStudent.setId(2L);

        // Both students match the (absent) search term; only "B.Sc IT" should survive the filter,
        // and it must be found even though the fast page-1 path would only have seen one student.
        when(studentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Sort.class)))
            .thenReturn(List.of(testStudent, otherStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.empty());
        when(allocationRepository.findByStudentId(2L)).thenReturn(Optional.empty());

        Page<FeeExplorerResponse.StudentFeeSummary> page = service.searchPageable(
            null, "B.Sc IT", null, null, null, PageRequest.of(0, 25));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).rollNumber()).isEqualTo("IT2024001");
    }

    @Test
    void shouldComputeFilterOptionsAcrossEveryStudentNotJustOnePage() {
        Program itProgram = new Program();
        itProgram.setId(2L);
        itProgram.setName("B.Sc IT");
        Student otherStudent = new Student("IT2024001", "Jane", "Roe", "jane@college.edu",
            itProgram, 3, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        otherStudent.setId(2L);

        AcademicYear ay = new AcademicYear();
        ay.setId(1L);
        ay.setName("2024-2025");
        Admission admission = new Admission(otherStudent, ay, LocalDate.of(2024, 6, 1));

        when(studentRepository.findAll()).thenReturn(List.of(testStudent, otherStudent));
        when(admissionRepository.findByStudentIdInFetchJoiningYear(any())).thenReturn(List.of(admission));

        FeeExplorerService.FilterOptions options = service.getFilterOptions();

        assertThat(options.programs()).containsExactly("B.Sc CS", "B.Sc IT");
        assertThat(options.academicYears()).containsExactly("2024-2025");
        assertThat(options.yearsOfStudy()).containsExactly(1, 3);
    }

    // ── Sem-wise export: one row per student per semester ──────────────────────────────────────

    @Test
    void shouldReturnOneRowPerSemesterWithFeePaidPending() {
        StudentFeeAllocation allocation = new StudentFeeAllocation(
            testStudent, testProgram, new BigDecimal("300000"),
            BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("300000"),
            FeeAllocationStatus.FINALIZED
        );
        allocation.setId(1L);

        SemesterFee sf1 = new SemesterFee(allocation, 1, "Sem 1", new BigDecimal("150000"), LocalDate.of(2024, 7, 31), 1);
        sf1.setId(1L);
        SemesterFee sf2 = new SemesterFee(allocation, 1, "Sem 2", new BigDecimal("150000"), LocalDate.of(2025, 1, 31), 2);
        sf2.setId(2L);

        when(studentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Sort.class)))
            .thenReturn(List.of(testStudent));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L))
            .thenReturn(List.of(sf1, sf2));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(new BigDecimal("150000"));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(2L)).thenReturn(new BigDecimal("50000"));
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of());
        when(penaltyRepository.findBySemesterFeeId(2L)).thenReturn(List.of());

        List<FeeExplorerSemesterWiseRow> rows =
            service.searchAllSemesterWise(null, null, null, null, null, Sort.by("rollNumber"));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).semesterLabel()).isEqualTo("Sem 1");
        assertThat(rows.get(0).fee()).isEqualByComparingTo("150000");
        assertThat(rows.get(0).paid()).isEqualByComparingTo("150000");
        assertThat(rows.get(0).pending()).isEqualByComparingTo("0");
        assertThat(rows.get(1).semesterLabel()).isEqualTo("Sem 2");
        assertThat(rows.get(1).fee()).isEqualByComparingTo("150000");
        assertThat(rows.get(1).paid()).isEqualByComparingTo("50000");
        assertThat(rows.get(1).pending()).isEqualByComparingTo("100000");
    }

    @Test
    void shouldSkipNotAllocatedStudentsInSemesterWiseExport() {
        when(studentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Sort.class)))
            .thenReturn(List.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.empty());

        List<FeeExplorerSemesterWiseRow> rows =
            service.searchAllSemesterWise(null, null, null, null, null, Sort.by("rollNumber"));

        assertThat(rows).isEmpty();
    }

    @Test
    void shouldCarryEnquiryCreditAcrossSemestersInSemesterWiseRows() {
        StudentFeeAllocation allocation = new StudentFeeAllocation(
            testStudent, testProgram, new BigDecimal("110000"),
            BigDecimal.ZERO, null, BigDecimal.ZERO, new BigDecimal("110000"),
            FeeAllocationStatus.FINALIZED
        );
        allocation.setId(1L);

        SemesterFee sf1 = new SemesterFee(allocation, 1, "Sem 1", new BigDecimal("55000"), LocalDate.of(2024, 7, 31), 1);
        sf1.setId(1L);
        SemesterFee sf2 = new SemesterFee(allocation, 1, "Sem 2", new BigDecimal("55000"), LocalDate.of(2025, 1, 31), 2);
        sf2.setId(2L);

        Enquiry enquiry = new Enquiry();
        enquiry.setId(8L);

        when(studentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Sort.class)))
            .thenReturn(List.of(testStudent));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(allocationRepository.findByStudentId(1L)).thenReturn(Optional.of(allocation));
        when(semesterFeeRepository.findByAllocationIdOrderByYearNumberAscSemesterSequenceAsc(1L))
            .thenReturn(List.of(sf1, sf2));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(1L)).thenReturn(new BigDecimal("50000"));
        when(installmentRepository.sumAmountPaidBySemesterFeeId(2L)).thenReturn(new BigDecimal("45000"));
        when(penaltyRepository.findBySemesterFeeId(1L)).thenReturn(List.of());
        when(penaltyRepository.findBySemesterFeeId(2L)).thenReturn(List.of());
        when(enquiryRepository.findByConvertedStudentId(1L)).thenReturn(Optional.of(enquiry));
        when(enquiryPaymentRepository.sumAmountPaidByEnquiryId(8L)).thenReturn(new BigDecimal("5000"));
        when(creditApplicationRepository.sumAmountAppliedByEnquiryId(8L)).thenReturn(new BigDecimal("5000"));
        when(creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(8L, 1L)).thenReturn(new BigDecimal("5000"));
        when(creditApplicationRepository.sumAmountAppliedByEnquiryIdAndSemesterFeeId(8L, 2L)).thenReturn(BigDecimal.ZERO);

        List<FeeExplorerSemesterWiseRow> rows =
            service.searchAllSemesterWise(null, null, null, null, null, Sort.by("rollNumber"));

        // Same figures as the aggregate-level equivalent test — semester 1 fully settled
        // (50000 installment + 5000 already-applied credit = 55000), semester 2 still owes 10000.
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).paid()).isEqualByComparingTo("55000");
        assertThat(rows.get(0).pending()).isEqualByComparingTo("0");
        assertThat(rows.get(1).paid()).isEqualByComparingTo("45000");
        assertThat(rows.get(1).pending()).isEqualByComparingTo("10000");
    }
}
