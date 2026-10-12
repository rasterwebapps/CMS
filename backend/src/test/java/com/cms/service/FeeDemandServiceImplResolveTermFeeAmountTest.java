package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.cms.model.AcademicYear;
import com.cms.model.Admission;
import com.cms.model.Cohort;
import com.cms.model.Course;
import com.cms.model.Enquiry;
import com.cms.model.FeeState;
import com.cms.model.FeeStructure;
import com.cms.model.FeeStructureGroup;
import com.cms.model.FeeStructureYearAmount;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.StudentTermFeeOverride;
import com.cms.model.TermInstance;
import com.cms.model.enums.AdmissionQuota;
import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FeeType;
import com.cms.model.enums.Gender;
import com.cms.model.enums.StudentType;
import com.cms.repository.AdmissionRepository;
import com.cms.repository.EnquiryPaymentRepository;
import com.cms.repository.EnquiryRepository;
import com.cms.repository.FeeDemandRepository;
import com.cms.repository.FeeStateRepository;
import com.cms.repository.FeeStructureGroupRepository;
import com.cms.repository.FeeStructureRepository;
import com.cms.repository.FeeStructureYearAmountRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.StudentTermFeeOverrideRepository;
import com.cms.repository.TermBillingScheduleRepository;
import com.cms.repository.TermInstanceRepository;
import com.cms.util.CurrentUserResolver;

/**
 * Covers {@link FeeDemandServiceImpl#resolveTermFeeAmount} -- the fix for OC-284's follow-up bug,
 * where the boarding-status switch's legacy-fee sync only updated the student's CURRENT term
 * (which has a real {@link StudentTermEnrollment} row) and silently skipped every later term
 * previewed in the same switch dialog (which, like any not-yet-generated future term, has no
 * enrollment row of its own yet). The fix mirrors {@code previewTermFeeSchedule}: reuse the
 * CURRENT enrollment's fee-structure group/academic year and only vary yearOfStudy/semesterNumber,
 * instead of requiring an enrollment matching the target semesterNumber.
 */
@ExtendWith(MockitoExtension.class)
class FeeDemandServiceImplResolveTermFeeAmountTest {

    @Mock private FeeDemandRepository feeDemandRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private StudentTermEnrollmentRepository enrollmentRepository;
    @Mock private FeeStructureGroupRepository feeStructureGroupRepository;
    @Mock private FeeStructureRepository feeStructureRepository;
    @Mock private FeeStructureYearAmountRepository yearAmountRepository;
    @Mock private TermBillingScheduleRepository billingScheduleRepository;
    @Mock private AdmissionRepository admissionRepository;
    @Mock private EnquiryPaymentRepository enquiryPaymentRepository;
    @Mock private StudentTermFeeOverrideRepository termFeeOverrideRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private EnquiryRepository enquiryRepository;
    @Mock private FeeStateRepository feeStateRepository;

    private FeeDemandServiceImpl service;

    private static final Long STUDENT_ID = 199L;

    private Student student;
    private Program program;
    private Cohort cohort;
    private AcademicYear academicYear;
    private StudentTermEnrollment currentEnrollment;
    private FeeStructureGroup group;

    @BeforeEach
    void setUp() {
        service = new FeeDemandServiceImpl(feeDemandRepository, termInstanceRepository, enrollmentRepository,
            feeStructureGroupRepository, feeStructureRepository, yearAmountRepository, billingScheduleRepository,
            admissionRepository, enquiryPaymentRepository, termFeeOverrideRepository, currentUserResolver,
            enquiryRepository, feeStateRepository);

        student = new Student();
        student.setId(STUDENT_ID);
        student.setGender(Gender.MALE);

        program = new Program();
        program.setId(1L);
        program.setCode("BSC-N");
        program.setDurationYears(4);
        program.setAssessmentPattern(AssessmentPattern.TERM_BASED);

        Course course = new Course();
        course.setId(5L);
        course.setProgram(program);

        cohort = new Cohort();
        cohort.setId(1L);
        cohort.setCourse(course);

        academicYear = new AcademicYear();
        academicYear.setId(10L);
        academicYear.setName("2026-27");

        TermInstance termInstance = new TermInstance();
        termInstance.setId(100L);
        termInstance.setAcademicYear(academicYear);

        currentEnrollment = new StudentTermEnrollment();
        currentEnrollment.setId(500L);
        currentEnrollment.setStudent(student);
        currentEnrollment.setCohort(cohort);
        currentEnrollment.setTermInstance(termInstance);
        currentEnrollment.setSemesterNumber(7);
        currentEnrollment.setYearOfStudy(4);
        currentEnrollment.setStatus(EnrollmentStatus.ENROLLED);

        group = new FeeStructureGroup();
        group.setId(900L);

        Admission admission = new Admission();
        admission.setId(1L);
        admission.setEnquiryId(1L);

        Enquiry enquiry = new Enquiry();
        enquiry.setAdmissionQuota(AdmissionQuota.MANAGEMENT);
        FeeState feeState = new FeeState();
        feeState.setId(1L);
        enquiry.setFeeState(feeState);

        when(enrollmentRepository.findByStudentIdAndStatus(STUDENT_ID, EnrollmentStatus.ENROLLED))
            .thenReturn(List.of(currentEnrollment));
        // Not every test reaches fee-structure-group resolution (override/no-enrollment short
        // circuits do not) -- lenient so those don't fail on an unused stub.
        lenient().when(admissionRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.of(admission));
        lenient().when(enquiryRepository.findById(1L)).thenReturn(Optional.of(enquiry));
        lenient().when(feeStructureGroupRepository.findExact(1L, 10L, 5L, AdmissionQuota.MANAGEMENT, 1L, Gender.MALE))
            .thenReturn(Optional.of(group));
    }

    private FeeStructure tuitionStructure() {
        FeeStructure fs = new FeeStructure();
        fs.setId(50L);
        fs.setFeeStructureGroup(group);
        fs.setFeeType(FeeType.TUITION);
        fs.setIsActive(true);
        return fs;
    }

    private FeeStructureYearAmount yearAmount(FeeStructure fs, int yearNumber, BigDecimal amount) {
        FeeStructureYearAmount ya = new FeeStructureYearAmount();
        ya.setFeeStructure(fs);
        ya.setYearNumber(yearNumber);
        ya.setAmount(amount);
        return ya;
    }

    @Test
    void futureTermWithNoOwnEnrollment_usesCurrentEnrollmentsFeeStructureGroup() {
        // Term 8 -- the student's only real enrollment is term 7 (current). This is the exact
        // shape of the bug: a future term previewed by the switch dialog but with no
        // StudentTermEnrollment row of its own yet.
        FeeStructure fs = tuitionStructure();
        when(feeStructureRepository.findByFeeStructureGroupIdAndIsActiveTrue(900L)).thenReturn(List.of(fs));
        when(yearAmountRepository.findByFeeStructureIdAndYearNumber(50L, 4))
            .thenReturn(List.of(yearAmount(fs, 4, new BigDecimal("145000"))));
        when(termFeeOverrideRepository.findByStudentIdAndSemesterNumber(STUDENT_ID, 8))
            .thenReturn(Optional.empty());

        BigDecimal result = service.resolveTermFeeAmount(STUDENT_ID, 8, StudentType.DAY_SCHOLAR);

        // yearOfStudy = ceil(8/2) = 4, semester 8 is the second term of the year => remainder half
        assertThat(result).isEqualByComparingTo("72500");
    }

    @Test
    void currentTermWithOverride_returnsOverrideWithoutCalculatingFeeStructure() {
        StudentTermFeeOverride override = new StudentTermFeeOverride();
        override.setOverrideAmount(new BigDecimal("72500"));
        when(termFeeOverrideRepository.findByStudentIdAndSemesterNumber(STUDENT_ID, 7))
            .thenReturn(Optional.of(override));

        BigDecimal result = service.resolveTermFeeAmount(STUDENT_ID, 7, StudentType.DAY_SCHOLAR);

        assertThat(result).isEqualByComparingTo("72500");
        verify(feeStructureRepository, never()).findByFeeStructureGroupIdAndIsActiveTrue(any());
    }

    @Test
    void noCurrentEnrollment_returnsNull() {
        when(enrollmentRepository.findByStudentIdAndStatus(STUDENT_ID, EnrollmentStatus.ENROLLED))
            .thenReturn(List.of());

        BigDecimal result = service.resolveTermFeeAmount(STUDENT_ID, 8, StudentType.DAY_SCHOLAR);

        assertThat(result).isNull();
    }

    @Test
    void noFeeStructureConfiguredForThatYear_returnsNullInsteadOfThrowing() {
        when(termFeeOverrideRepository.findByStudentIdAndSemesterNumber(STUDENT_ID, 8))
            .thenReturn(Optional.empty());
        when(feeStructureRepository.findByFeeStructureGroupIdAndIsActiveTrue(900L)).thenReturn(List.of());

        BigDecimal result = service.resolveTermFeeAmount(STUDENT_ID, 8, StudentType.DAY_SCHOLAR);

        assertThat(result).isNull();
    }
}
