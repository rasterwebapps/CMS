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

import com.cms.dto.ScholarshipApplicationRequest;
import com.cms.dto.ScholarshipApprovalRequest;
import com.cms.dto.ScholarshipRejectionRequest;
import com.cms.model.AcademicYear;
import com.cms.model.Program;
import com.cms.model.ScholarshipType;
import com.cms.model.Student;
import com.cms.model.StudentScholarship;
import com.cms.model.StudentScholarshipEligibility;
import com.cms.model.enums.DiscountType;
import com.cms.model.enums.DisbursementFrequency;
import com.cms.model.enums.ScholarshipStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.ScholarshipTypeRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.StudentScholarshipEligibilityRepository;
import com.cms.repository.StudentScholarshipRepository;

@ExtendWith(MockitoExtension.class)
class StudentScholarshipServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private ScholarshipTypeRepository scholarshipTypeRepository;
    @Mock private StudentScholarshipRepository applicationRepository;
    @Mock private StudentScholarshipEligibilityRepository eligibilityRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private ScholarshipTypeService scholarshipTypeService;
    private StudentScholarshipService service;
    private Student student;
    private AcademicYear currentYear;
    private ScholarshipType scType;

    @BeforeEach
    void setUp() {
        service = new StudentScholarshipService(studentRepository, scholarshipTypeRepository, applicationRepository,
            eligibilityRepository, academicYearRepository, scholarshipTypeService);
        Program program = new Program("Bachelor", "BACH", 4);
        program.setId(1L);
        student = new Student("S1", "A", "B", "a@test.com", program, 1, LocalDate.now(), StudentStatus.ACTIVE);
        student.setId(1L);
        student.setCommunityCategory("SC");
        currentYear = new AcademicYear("2026-2027", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), true);
        currentYear.setId(10L);
        scType = type(2L, "SC_GOVT", DiscountType.PERCENTAGE, new BigDecimal("100"), true);
    }

    @Test
    void shouldDetectEligibleScholarships() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(eligibilityRepository.findByStudentId(1L)).thenReturn(Optional.empty());
        when(scholarshipTypeRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(scType));
        when(scholarshipTypeService.toResponse(scType)).thenReturn(new com.cms.dto.ScholarshipTypeResponse(
            2L, "SC_GOVT", "SC_GOVT", null, true, null, DiscountType.PERCENTAGE, new BigDecimal("100"), null, true, true,
            com.cms.model.enums.ScholarshipApplicationMode.GOVT_PORTAL, "ePass TN", "https://tnepass.tn.gov.in", null, null,
            null, null));
        assertThat(service.getEligibleScholarships(1L)).extracting("code").containsExactly("SC_GOVT");
    }

    @Test
    void shouldApplyForScholarshipAndPreventDuplicateYear() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(academicYearRepository.findByIsCurrentTrue()).thenReturn(Optional.of(currentYear));
        when(applicationRepository.existsByStudentIdAndAcademicYearId(1L, 10L)).thenReturn(false, true);
        when(scholarshipTypeRepository.findById(2L)).thenReturn(Optional.of(scType));
        when(applicationRepository.save(any(StudentScholarship.class))).thenAnswer(inv -> { StudentScholarship a = inv.getArgument(0); a.setId(3L); return a; });
        var response = service.applyForScholarship(1L, new ScholarshipApplicationRequest(2L, null, "Apply"), "admin");
        assertThat(response.status()).isEqualTo(ScholarshipStatus.PENDING);
        assertThatThrownBy(() -> service.applyForScholarship(1L, new ScholarshipApplicationRequest(2L, null, null), "admin"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldApproveRejectCancelAndRenew() {
        StudentScholarship app = app(ScholarshipStatus.PENDING);
        when(applicationRepository.findById(3L)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(StudentScholarship.class))).thenAnswer(inv -> inv.getArgument(0));
        var approved = service.approveScholarship(3L, new ScholarshipApprovalRequest(new BigDecimal("5000"), null, null, null, null), "admin");
        assertThat(approved.status()).isEqualTo(ScholarshipStatus.APPROVED);
        assertThat(approved.validFrom()).isEqualTo(currentYear.getStartDate());

        StudentScholarship pending = app(ScholarshipStatus.PENDING);
        when(applicationRepository.findById(4L)).thenReturn(Optional.of(pending));
        assertThat(service.rejectScholarship(4L, new ScholarshipRejectionRequest("No docs"), "admin").status()).isEqualTo(ScholarshipStatus.REJECTED);

        StudentScholarship cancel = app(ScholarshipStatus.PENDING);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(cancel));
        assertThat(service.cancelScholarship(5L, "admin").status()).isEqualTo(ScholarshipStatus.CANCELLED);

        AcademicYear next = new AcademicYear("2027-2028", LocalDate.of(2027, 6, 1), LocalDate.of(2028, 5, 31), false);
        next.setId(11L);
        StudentScholarship source = app(ScholarshipStatus.APPROVED);
        when(applicationRepository.findById(6L)).thenReturn(Optional.of(source));
        when(academicYearRepository.findByName("2027-2028")).thenReturn(Optional.of(next));
        when(applicationRepository.existsByStudentIdAndAcademicYearId(1L, 11L)).thenReturn(false);
        assertThat(service.renewScholarship(6L, "admin").status()).isEqualTo(ScholarshipStatus.PENDING);
    }

    @Test
    void shouldCalculateAmountsAndFindApprovedCurrentYear() {
        assertThat(service.calculateScholarshipAmount(new BigDecimal("10000"), scType)).isEqualByComparingTo("10000.00");
        ScholarshipType fixed = type(7L, "FIXED", DiscountType.FIXED_AMOUNT, new BigDecimal("2000"), false);
        fixed.setMaxAmountPerYear(new BigDecimal("1000"));
        assertThat(service.calculateScholarshipAmount(new BigDecimal("10000"), fixed)).isEqualByComparingTo("1000");
        ScholarshipType full = type(8L, "FULL", DiscountType.FULL_WAIVER, null, false);
        assertThat(service.calculateScholarshipAmount(new BigDecimal("10000"), full)).isEqualByComparingTo("10000");

        StudentScholarship approved = app(ScholarshipStatus.APPROVED);
        when(academicYearRepository.findByIsCurrentTrue()).thenReturn(Optional.of(currentYear));
        when(applicationRepository.findByStudentIdAndAcademicYearId(1L, 10L)).thenReturn(Optional.of(approved));
        assertThat(service.findApprovedForStudentInCurrentYear(1L)).contains(approved);
    }

    private StudentScholarship app(ScholarshipStatus status) {
        StudentScholarship app = new StudentScholarship();
        app.setId(3L);
        app.setStudent(student);
        app.setScholarshipType(scType);
        app.setAcademicYear(currentYear);
        app.setStatus(status);
        app.setApplicationDate(LocalDate.now());
        app.setDisbursementFrequency(DisbursementFrequency.ANNUAL);
        return app;
    }

    private ScholarshipType type(Long id, String code, DiscountType discountType, BigDecimal value, boolean renewalRequired) {
        ScholarshipType t = new ScholarshipType();
        t.setId(id);
        t.setCode(code);
        t.setName(code);
        t.setDiscountType(discountType);
        t.setDiscountValue(value);
        t.setRenewalRequired(renewalRequired);
        t.setActive(true);
        t.setGovtScheme(true);
        return t;
    }
}

