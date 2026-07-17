package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.cms.dto.CohortTermOption;
import com.cms.dto.PromotionDecisionInput;
import com.cms.dto.PromotionExecuteRequest;
import com.cms.dto.PromotionExecuteResponse;
import com.cms.dto.PromotionPreviewRequest;
import com.cms.dto.PromotionPreviewResponse;
import com.cms.dto.StudentPromotionPreviewRow;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Course;
import com.cms.model.CourseOffering;
import com.cms.model.CourseRegistration;
import com.cms.model.CurriculumVersion;
import com.cms.model.ExamResult;
import com.cms.model.Examination;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentPromotionDecision;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.ExamOutcome;
import com.cms.model.enums.ExamResultStatus;
import com.cms.model.enums.ExamType;
import com.cms.model.enums.PromotionOutcome;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.RegistrationStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.ExamResultRepository;
import com.cms.repository.StudentPromotionDecisionRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.SubjectRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class StudentPromotionServiceImplTest {

    @Mock private CohortRepository cohortRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private StudentTermEnrollmentRepository enrollmentRepository;
    @Mock private CourseRegistrationRepository courseRegistrationRepository;
    @Mock private ExamResultRepository examResultRepository;
    @Mock private StudentPromotionDecisionRepository decisionRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private AttendanceService attendanceService;
    @Mock private CourseRegistrationService courseRegistrationService;
    @Mock private FeeDemandService feeDemandService;

    private StudentPromotionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentPromotionServiceImpl(
            cohortRepository, termInstanceRepository, academicYearRepository, enrollmentRepository,
            courseRegistrationRepository, examResultRepository, decisionRepository, studentRepository,
            subjectRepository, attendanceService, courseRegistrationService, feeDemandService);
    }

    private AcademicYear createAY(Long id, String name, int startYear) {
        AcademicYear ay = new AcademicYear(name, LocalDate.of(startYear, 6, 1), LocalDate.of(startYear + 1, 5, 31), false);
        ay.setId(id);
        return ay;
    }

    private Program createProgram(Long id, int durationYears) {
        Program p = new Program("Program " + id, "P" + id, durationYears, ProgramStatus.ACTIVE);
        p.setId(id);
        return p;
    }

    private Course createCourse(Long id, Program program) {
        Course c = new Course("Course " + id, "C" + id, null, program);
        c.setId(id);
        return c;
    }

    private Cohort createCohort(Long id, Course course, AcademicYear admissionAy) {
        Cohort c = new Cohort();
        c.setId(id);
        c.setCourse(course);
        c.setAdmissionAcademicYear(admissionAy);
        c.setCohortCode("COHORT-" + id);
        c.setDisplayName("Cohort " + id);
        c.setStatus(CohortStatus.ACTIVE);
        return c;
    }

    private TermInstance createTermInstance(Long id, AcademicYear ay, TermType type) {
        TermInstance ti = new TermInstance(ay, type, ay.getStartDate(), ay.getEndDate(), TermInstanceStatus.OPEN);
        ti.setId(id);
        return ti;
    }

    private Student createStudent(Long id) {
        Student s = new Student("ROLL" + id, "Student", String.valueOf(id), "s" + id + "@test.com",
            null, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        s.setId(id);
        return s;
    }

    private StudentTermEnrollment createEnrollment(Long id, Student student, TermInstance ti, Cohort cohort, int semNum) {
        StudentTermEnrollment e = new StudentTermEnrollment();
        e.setId(id);
        e.setStudent(student);
        e.setTermInstance(ti);
        e.setCohort(cohort);
        e.setSemesterNumber(semNum);
        e.setYearOfStudy((int) Math.ceil(semNum / 2.0));
        e.setStatus(EnrollmentStatus.ENROLLED);
        return e;
    }

    private Subject createSubject(Long id, String name, String code) {
        Subject s = new Subject();
        s.setId(id);
        s.setName(name);
        s.setCode(code);
        return s;
    }

    private CourseRegistration createRegistration(Long id, StudentTermEnrollment enrollment, Subject subject,
                                                    CurriculumVersion cv, TermInstance ti) {
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setTermInstance(ti);
        offering.setCurriculumVersion(cv);
        offering.setSubject(subject);
        offering.setSemesterNumber(enrollment.getSemesterNumber());
        offering.setIsActive(true);

        CourseRegistration reg = new CourseRegistration();
        reg.setId(id);
        reg.setStudentTermEnrollment(enrollment);
        reg.setCourseOffering(offering);
        reg.setStatus(RegistrationStatus.REGISTERED);
        return reg;
    }

    private ExamResult createPublishedResult(Long id, Student student, Subject subject, LocalDate date, ExamOutcome outcome) {
        Examination exam = new Examination("Exam " + id, subject, ExamType.THEORY, date, 120, 100);
        exam.setId(id);
        ExamResult result = new ExamResult(exam, student, null, null, ExamResultStatus.PUBLISHED);
        result.setId(id);
        result.setOutcome(outcome);
        return result;
    }

    @Test
    void getActiveTermsForCohort_groupsAndCountsByTermInstance() {
        AcademicYear ay = createAY(1L, "2024-2025", 2024);
        Program program = createProgram(1L, 4);
        Course course = createCourse(1L, program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance term1 = createTermInstance(1L, ay, TermType.ODD);
        Student studentA = createStudent(1L);
        Student studentB = createStudent(2L);
        StudentTermEnrollment enrollmentA = createEnrollment(1L, studentA, term1, cohort, 1);
        StudentTermEnrollment enrollmentB = createEnrollment(2L, studentB, term1, cohort, 1);

        when(cohortRepository.existsById(1L)).thenReturn(true);
        when(enrollmentRepository.findByCohortIdAndStatus(1L, EnrollmentStatus.ENROLLED))
            .thenReturn(List.of(enrollmentA, enrollmentB));

        List<CohortTermOption> options = service.getActiveTermsForCohort(1L);

        assertThat(options).hasSize(1);
        assertThat(options.get(0).termInstanceId()).isEqualTo(1L);
        assertThat(options.get(0).enrolledCount()).isEqualTo(2);
    }

    @Test
    void suggestNextTerm_oddToEvenSameYear() {
        AcademicYear ay = createAY(1L, "2024-2025", 2024);
        TermInstance fromTerm = createTermInstance(1L, ay, TermType.ODD);
        TermInstance evenTerm = createTermInstance(2L, ay, TermType.EVEN);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(fromTerm));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(1L, TermType.EVEN))
            .thenReturn(Optional.of(evenTerm));

        CohortTermOption suggestion = service.suggestNextTerm(1L);

        assertThat(suggestion).isNotNull();
        assertThat(suggestion.termInstanceId()).isEqualTo(2L);
    }

    @Test
    void suggestNextTerm_evenToOddNextYear() {
        AcademicYear ay = createAY(1L, "2024-2025", 2024);
        AcademicYear nextAy = createAY(2L, "2025-2026", 2025);
        TermInstance fromTerm = createTermInstance(1L, ay, TermType.EVEN);
        TermInstance nextOddTerm = createTermInstance(2L, nextAy, TermType.ODD);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(fromTerm));
        when(academicYearRepository.findByStartDateGreaterThanOrderByStartDateAsc(ay.getStartDate()))
            .thenReturn(List.of(nextAy));
        when(termInstanceRepository.findByAcademicYearIdAndTermType(2L, TermType.ODD))
            .thenReturn(Optional.of(nextOddTerm));

        CohortTermOption suggestion = service.suggestNextTerm(1L);

        assertThat(suggestion).isNotNull();
        assertThat(suggestion.termInstanceId()).isEqualTo(2L);
    }

    @Test
    void suggestNextTerm_returnsNullWhenNoNextAcademicYearExists() {
        AcademicYear ay = createAY(1L, "2024-2025", 2024);
        TermInstance fromTerm = createTermInstance(1L, ay, TermType.EVEN);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(fromTerm));
        when(academicYearRepository.findByStartDateGreaterThanOrderByStartDateAsc(ay.getStartDate()))
            .thenReturn(List.of());

        CohortTermOption suggestion = service.suggestNextTerm(1L);

        assertThat(suggestion).isNull();
    }

    @Test
    void previewPromotion_recommendsPromotedWhenNoArrearsOrLowAttendance() {
        AcademicYear ay = createAY(1L, "2024-2025", 2024);
        Program program = createProgram(1L, 4);
        Course course = createCourse(1L, program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance fromTerm = createTermInstance(1L, ay, TermType.ODD);
        TermInstance toTerm = createTermInstance(2L, ay, TermType.EVEN);
        Student student = createStudent(1L);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, fromTerm, cohort, 1);

        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(fromTerm));
        when(termInstanceRepository.findById(2L)).thenReturn(Optional.of(toTerm));
        when(enrollmentRepository.findByTermInstanceIdAndCohortId(1L, 1L)).thenReturn(List.of(enrollment));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of());
        when(examResultRepository.findByStudentId(1L)).thenReturn(List.of());

        PromotionPreviewResponse response = service.previewPromotion(new PromotionPreviewRequest(1L, 1L, 2L));

        assertThat(response.students()).hasSize(1);
        StudentPromotionPreviewRow row = response.students().get(0);
        assertThat(row.recommendedOutcome()).isEqualTo(PromotionOutcome.PROMOTED);
        assertThat(row.blockReasons()).isEmpty();
        assertThat(row.totalArrearSubjects()).isEmpty();
    }

    @Test
    void previewPromotion_recommendsPromotedWithArrearsWhenSubjectFailed() {
        AcademicYear ay = createAY(1L, "2024-2025", 2024);
        Program program = createProgram(1L, 4);
        Course course = createCourse(1L, program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance fromTerm = createTermInstance(1L, ay, TermType.ODD);
        TermInstance toTerm = createTermInstance(2L, ay, TermType.EVEN);
        Student student = createStudent(1L);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, fromTerm, cohort, 1);
        Subject subject = createSubject(1L, "Anatomy", "ANAT101");
        CourseRegistration registration = createRegistration(1L, enrollment, subject, null, fromTerm);
        ExamResult failedResult = createPublishedResult(1L, student, subject, LocalDate.of(2024, 10, 1), ExamOutcome.FAIL);

        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(fromTerm));
        when(termInstanceRepository.findById(2L)).thenReturn(Optional.of(toTerm));
        when(enrollmentRepository.findByTermInstanceIdAndCohortId(1L, 1L)).thenReturn(List.of(enrollment));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of(registration));
        when(attendanceService.getAttendanceReport(1L, 1L)).thenReturn(List.of());
        when(examResultRepository.findByStudentId(1L)).thenReturn(List.of(failedResult));

        PromotionPreviewResponse response = service.previewPromotion(new PromotionPreviewRequest(1L, 1L, 2L));

        StudentPromotionPreviewRow row = response.students().get(0);
        assertThat(row.recommendedOutcome()).isEqualTo(PromotionOutcome.PROMOTED_WITH_ARREARS);
        assertThat(row.newArrearSubjects()).extracting("subjectId").containsExactly(1L);
        assertThat(row.totalArrearSubjects()).extracting("subjectId").containsExactly(1L);
        assertThat(row.blockReasons()).isEmpty();
    }

    @Test
    void previewPromotion_blocksAtFinalYearGateWithCarriedArrears() {
        AcademicYear ay = createAY(1L, "2024-2025", 2024);
        Program program = createProgram(1L, 4); // TERM_BASED -> totalTerms = 8
        Course course = createCourse(1L, program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance fromTerm = createTermInstance(1L, ay, TermType.ODD); // semNum 7 -> next = 8 (final)
        TermInstance toTerm = createTermInstance(2L, ay, TermType.EVEN);
        Student student = createStudent(1L);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, fromTerm, cohort, 7);
        Subject carriedSubject = createSubject(2L, "Pharmacology", "PHARM101");
        ExamResult carriedFail = createPublishedResult(2L, student, carriedSubject, LocalDate.of(2022, 10, 1), ExamOutcome.FAIL);

        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(fromTerm));
        when(termInstanceRepository.findById(2L)).thenReturn(Optional.of(toTerm));
        when(enrollmentRepository.findByTermInstanceIdAndCohortId(1L, 1L)).thenReturn(List.of(enrollment));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of());
        when(examResultRepository.findByStudentId(1L)).thenReturn(List.of(carriedFail));

        PromotionPreviewResponse response = service.previewPromotion(new PromotionPreviewRequest(1L, 1L, 2L));

        StudentPromotionPreviewRow row = response.students().get(0);
        assertThat(row.blockReasons()).contains("ARREARS_AT_FINAL_YEAR_GATE");
        assertThat(row.recommendedOutcome()).isNull();
    }

    @Test
    void previewPromotion_blocksOnMaxDurationExceeded() {
        AcademicYear admissionAy = createAY(1L, "2020-2021", 2020);
        AcademicYear toAy = createAY(2L, "2028-2029", 2028); // 2028 - 2020 = 8 >= maxDurationYears(8)
        Program program = createProgram(1L, 4);
        Course course = createCourse(1L, program);
        Cohort cohort = createCohort(1L, course, admissionAy);
        TermInstance fromTerm = createTermInstance(1L, admissionAy, TermType.ODD);
        TermInstance toTerm = createTermInstance(2L, toAy, TermType.ODD);
        Student student = createStudent(1L);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, fromTerm, cohort, 1);

        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(fromTerm));
        when(termInstanceRepository.findById(2L)).thenReturn(Optional.of(toTerm));
        when(enrollmentRepository.findByTermInstanceIdAndCohortId(1L, 1L)).thenReturn(List.of(enrollment));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of());
        when(examResultRepository.findByStudentId(1L)).thenReturn(List.of());

        PromotionPreviewResponse response = service.previewPromotion(new PromotionPreviewRequest(1L, 1L, 2L));

        StudentPromotionPreviewRow row = response.students().get(0);
        assertThat(row.blockReasons()).contains("MAX_DURATION_EXCEEDED");
        assertThat(row.recommendedOutcome()).isNull();
    }

    @Test
    void executePromotion_promotesAndCreatesNextEnrollment() {
        AcademicYear ay = createAY(1L, "2024-2025", 2024);
        Program program = createProgram(1L, 4);
        Course course = createCourse(1L, program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance fromTerm = createTermInstance(1L, ay, TermType.ODD);
        TermInstance toTerm = createTermInstance(2L, ay, TermType.EVEN);
        Student student = createStudent(1L);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, fromTerm, cohort, 1);

        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(fromTerm));
        when(termInstanceRepository.findById(2L)).thenReturn(Optional.of(toTerm));
        when(enrollmentRepository.findByTermInstanceIdAndCohortId(1L, 1L)).thenReturn(List.of(enrollment));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of());
        when(examResultRepository.findByStudentId(1L)).thenReturn(List.of());
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.findByStudentIdAndTermInstanceId(1L, 2L)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(StudentTermEnrollment.class))).thenAnswer(inv -> {
            StudentTermEnrollment e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(2L);
            }
            return e;
        });
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        when(decisionRepository.save(any(StudentPromotionDecision.class))).thenAnswer(inv -> inv.getArgument(0));

        PromotionExecuteRequest request = new PromotionExecuteRequest(1L, 1L, 2L,
            List.of(new PromotionDecisionInput(1L, PromotionOutcome.PROMOTED, null)), false, false);

        PromotionExecuteResponse response = service.executePromotion(request, "admin@test.com");

        assertThat(response.promotedCount()).isEqualTo(1);
        assertThat(response.rejectedDecisions()).isEmpty();
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(student.getSemester()).isEqualTo(1);

        ArgumentCaptor<StudentPromotionDecision> captor = ArgumentCaptor.forClass(StudentPromotionDecision.class);
        verify(decisionRepository).save(captor.capture());
        assertThat(captor.getValue().getOutcome()).isEqualTo(PromotionOutcome.PROMOTED);
        assertThat(captor.getValue().getToTermInstance()).isEqualTo(toTerm);
    }

    @Test
    void executePromotion_detainedRepeatDoesNotCreateNextEnrollment() {
        AcademicYear ay = createAY(1L, "2024-2025", 2024);
        Program program = createProgram(1L, 4);
        Course course = createCourse(1L, program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance fromTerm = createTermInstance(1L, ay, TermType.ODD);
        TermInstance toTerm = createTermInstance(2L, ay, TermType.EVEN);
        Student student = createStudent(1L);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, fromTerm, cohort, 1);

        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(fromTerm));
        when(termInstanceRepository.findById(2L)).thenReturn(Optional.of(toTerm));
        when(enrollmentRepository.findByTermInstanceIdAndCohortId(1L, 1L)).thenReturn(List.of(enrollment));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of());
        when(examResultRepository.findByStudentId(1L)).thenReturn(List.of());
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(decisionRepository.save(any(StudentPromotionDecision.class))).thenAnswer(inv -> inv.getArgument(0));

        PromotionExecuteRequest request = new PromotionExecuteRequest(1L, 1L, 2L,
            List.of(new PromotionDecisionInput(1L, PromotionOutcome.DETAINED_REPEAT, "Low attendance")), false, false);

        PromotionExecuteResponse response = service.executePromotion(request, "admin@test.com");

        assertThat(response.detainedCount()).isEqualTo(1);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        verify(enrollmentRepository, never()).findByStudentIdAndTermInstanceId(1L, 2L);

        ArgumentCaptor<StudentPromotionDecision> captor = ArgumentCaptor.forClass(StudentPromotionDecision.class);
        verify(decisionRepository).save(captor.capture());
        assertThat(captor.getValue().getOutcome()).isEqualTo(PromotionOutcome.DETAINED_REPEAT);
        assertThat(captor.getValue().getToTermInstance()).isNull();
    }

    @Test
    void executePromotion_excludedWritesNoDecisionRow() {
        AcademicYear ay = createAY(1L, "2024-2025", 2024);
        Program program = createProgram(1L, 4);
        Course course = createCourse(1L, program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance fromTerm = createTermInstance(1L, ay, TermType.ODD);
        TermInstance toTerm = createTermInstance(2L, ay, TermType.EVEN);
        Student student = createStudent(1L);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, fromTerm, cohort, 1);

        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(fromTerm));
        when(termInstanceRepository.findById(2L)).thenReturn(Optional.of(toTerm));
        when(enrollmentRepository.findByTermInstanceIdAndCohortId(1L, 1L)).thenReturn(List.of(enrollment));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of());
        when(examResultRepository.findByStudentId(1L)).thenReturn(List.of());
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        PromotionExecuteRequest request = new PromotionExecuteRequest(1L, 1L, 2L,
            List.of(new PromotionDecisionInput(1L, PromotionOutcome.EXCLUDED, null)), false, false);

        PromotionExecuteResponse response = service.executePromotion(request, "admin@test.com");

        assertThat(response.excludedCount()).isEqualTo(1);
        verify(decisionRepository, never()).save(any());
    }

    @Test
    void executePromotion_rejectsPromotedDecisionWhenBlocked() {
        AcademicYear admissionAy = createAY(1L, "2020-2021", 2020);
        AcademicYear toAy = createAY(2L, "2028-2029", 2028);
        Program program = createProgram(1L, 4);
        Course course = createCourse(1L, program);
        Cohort cohort = createCohort(1L, course, admissionAy);
        TermInstance fromTerm = createTermInstance(1L, admissionAy, TermType.ODD);
        TermInstance toTerm = createTermInstance(2L, toAy, TermType.ODD);
        Student student = createStudent(1L);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, fromTerm, cohort, 1);

        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(fromTerm));
        when(termInstanceRepository.findById(2L)).thenReturn(Optional.of(toTerm));
        when(enrollmentRepository.findByTermInstanceIdAndCohortId(1L, 1L)).thenReturn(List.of(enrollment));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of());
        when(examResultRepository.findByStudentId(1L)).thenReturn(List.of());

        PromotionExecuteRequest request = new PromotionExecuteRequest(1L, 1L, 2L,
            List.of(new PromotionDecisionInput(1L, PromotionOutcome.PROMOTED, null)), false, false);

        PromotionExecuteResponse response = service.executePromotion(request, "admin@test.com");

        assertThat(response.promotedCount()).isEqualTo(0);
        assertThat(response.rejectedDecisions()).hasSize(1);
        assertThat(response.rejectedDecisions().get(0).reason()).contains("MAX_DURATION_EXCEEDED");
        verify(decisionRepository, never()).save(any());
        verify(enrollmentRepository, never()).findById(any());
    }

    @Test
    void executePromotion_graduatesFinalTermStudentWithNoArrears() {
        AcademicYear ay = createAY(1L, "2024-2025", 2024);
        Program program = createProgram(1L, 4); // totalTerms = 8
        Course course = createCourse(1L, program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance fromTerm = createTermInstance(1L, ay, TermType.EVEN);
        TermInstance toTerm = createTermInstance(2L, ay, TermType.EVEN);
        Student student = createStudent(1L);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, fromTerm, cohort, 8); // final term

        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(fromTerm));
        when(termInstanceRepository.findById(2L)).thenReturn(Optional.of(toTerm));
        when(enrollmentRepository.findByTermInstanceIdAndCohortId(1L, 1L)).thenReturn(List.of(enrollment));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of());
        when(examResultRepository.findByStudentId(1L)).thenReturn(List.of());
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        when(decisionRepository.save(any(StudentPromotionDecision.class))).thenAnswer(inv -> inv.getArgument(0));

        PromotionExecuteRequest request = new PromotionExecuteRequest(1L, 1L, 2L,
            List.of(new PromotionDecisionInput(1L, PromotionOutcome.GRADUATED, null)), false, false);

        PromotionExecuteResponse response = service.executePromotion(request, "admin@test.com");

        assertThat(response.graduatedCount()).isEqualTo(1);
        assertThat(student.getStatus()).isEqualTo(StudentStatus.GRADUATED);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        verify(enrollmentRepository, never()).findByStudentIdAndTermInstanceId(1L, 2L);
    }
}
