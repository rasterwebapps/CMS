package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.TermInstance;
import com.cms.model.enums.AssessmentPattern;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.CohortRepository;
import com.cms.repository.StudentRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class StudentTermEnrollmentServiceImplTest {

    @Mock
    private StudentTermEnrollmentRepository enrollmentRepository;
    @Mock
    private TermInstanceRepository termInstanceRepository;
    @Mock
    private CohortRepository cohortRepository;
    @Mock
    private StudentRepository studentRepository;

    private StudentTermEnrollmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentTermEnrollmentServiceImpl(
            enrollmentRepository, termInstanceRepository, cohortRepository, studentRepository);
    }

    private AcademicYear createAY(Long id, String name) {
        AcademicYear ay = new AcademicYear(name, LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(id);
        return ay;
    }

    private Program createProgram(Long id, String code, int durationYears) {
        Program p = new Program(code + " Program", code, durationYears, ProgramStatus.ACTIVE);
        p.setId(id);
        return p;
    }

    private Program createYearlyProgram(Long id, String code, int durationYears) {
        Program p = new Program(code + " Program", code, durationYears, ProgramStatus.ACTIVE,
            AssessmentPattern.YEARLY);
        p.setId(id);
        return p;
    }

    private Cohort createCohort(Long id, Program program, AcademicYear admissionAY) {
        Cohort c = new Cohort();
        c.setId(id);
        c.setProgram(program);
        c.setAdmissionAcademicYear(admissionAY);
        c.setCohortCode(program.getCode() + "-2024-2027");
        c.setDisplayName(program.getName() + " (2024-2027)");
        c.setStatus(CohortStatus.ACTIVE);
        return c;
    }

    private TermInstance createTermInstance(Long id, AcademicYear ay, TermType termType) {
        TermInstance ti = new TermInstance(ay, termType, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 11, 30),
            TermInstanceStatus.OPEN);
        ti.setId(id);
        return ti;
    }

    private Student createStudent(Long id, Program program, Cohort cohort) {
        Student s = new Student("ROLL00" + id, "Student", "One", "s" + id + "@test.com",
            program, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        s.setId(id);
        s.setCohort(cohort);
        return s;
    }

    @Test
    void generateEnrollmentsForTermInstance_createsEnrollments() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(termInstance));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(studentRepository.findByCohortIdAndStatus(1L, StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentRepository.findByStudentIdAndTermInstanceId(1L, 1L)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(StudentTermEnrollment.class))).thenAnswer(inv -> {
            StudentTermEnrollment e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        int count = service.generateEnrollmentsForTermInstance(1L);

        assertThat(count).isEqualTo(1);
        verify(enrollmentRepository).save(any(StudentTermEnrollment.class));
    }

    @Test
    void generateEnrollmentsForTermInstance_skipsExistingEnrollments() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment existing = new StudentTermEnrollment();
        existing.setId(99L);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(termInstance));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(studentRepository.findByCohortIdAndStatus(1L, StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentRepository.findByStudentIdAndTermInstanceId(1L, 1L)).thenReturn(Optional.of(existing));

        int count = service.generateEnrollmentsForTermInstance(1L);

        assertThat(count).isEqualTo(0);
        verify(enrollmentRepository, never()).save(any(StudentTermEnrollment.class));
    }

    @Test
    void generateEnrollmentsForTermInstance_skipsOutOfRangeSemesters() {
        AcademicYear admissionAY = createAY(1L, "2024-2025");
        AcademicYear currentAY = createAY(2L, "2027-2028");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, admissionAY);
        TermInstance termInstance = createTermInstance(2L, currentAY, TermType.ODD);

        when(termInstanceRepository.findById(2L)).thenReturn(Optional.of(termInstance));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));

        int count = service.generateEnrollmentsForTermInstance(2L);

        assertThat(count).isEqualTo(0);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void computeSemesterNumber_oddTerm_firstYear() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);

        Integer result = service.computeSemesterNumber(cohort, termInstance);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void computeSemesterNumber_evenTerm_firstYear() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.EVEN);

        Integer result = service.computeSemesterNumber(cohort, termInstance);

        assertThat(result).isEqualTo(2);
    }

    @Test
    void computeSemesterNumber_oddTerm_secondYear() {
        AcademicYear admissionAY = createAY(1L, "2024-2025");
        AcademicYear secondYearAY = createAY(2L, "2025-2026");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, admissionAY);
        TermInstance termInstance = createTermInstance(2L, secondYearAY, TermType.ODD);

        Integer result = service.computeSemesterNumber(cohort, termInstance);

        assertThat(result).isEqualTo(3);
    }

    @Test
    void computeSemesterNumber_outOfRange_returnsNull() {
        AcademicYear admissionAY = createAY(1L, "2024-2025");
        AcademicYear farFutureAY = createAY(2L, "2027-2028");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, admissionAY);
        TermInstance termInstance = createTermInstance(2L, farFutureAY, TermType.ODD);

        Integer result = service.computeSemesterNumber(cohort, termInstance);

        assertThat(result).isNull();
    }

    @Test
    void getEnrollmentsByTermInstance_returnsMappedDtos() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);

        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setId(1L);
        enrollment.setStudent(student);
        enrollment.setTermInstance(termInstance);
        enrollment.setCohort(cohort);
        enrollment.setSemesterNumber(1);
        enrollment.setYearOfStudy(1);
        enrollment.setStatus(com.cms.model.enums.EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByTermInstanceId(1L)).thenReturn(List.of(enrollment));

        var result = service.getEnrollmentsByTermInstance(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).termNumber()).isEqualTo(1);
        assertThat(result.get(0).studentName()).isEqualTo("Student One");
    }

    @Test
    void getEnrollmentsByStudent_returnsMappedDtos() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);

        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setId(1L);
        enrollment.setStudent(student);
        enrollment.setTermInstance(termInstance);
        enrollment.setCohort(cohort);
        enrollment.setSemesterNumber(1);
        enrollment.setYearOfStudy(1);
        enrollment.setStatus(com.cms.model.enums.EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment));

        var result = service.getEnrollmentsByStudent(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentId()).isEqualTo(1L);
    }

    @Test
    void getEnrollmentsByTermInstanceAndSemester_returnsMappedDtos() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);

        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setId(1L);
        enrollment.setStudent(student);
        enrollment.setTermInstance(termInstance);
        enrollment.setCohort(cohort);
        enrollment.setSemesterNumber(1);
        enrollment.setYearOfStudy(1);
        enrollment.setStatus(com.cms.model.enums.EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByTermInstanceIdAndSemesterNumber(1L, 1)).thenReturn(List.of(enrollment));

        var result = service.getEnrollmentsByTermInstanceAndSemester(1L, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).termNumber()).isEqualTo(1);
    }

    @Test
    void getById_returnsDto() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);

        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setId(1L);
        enrollment.setStudent(student);
        enrollment.setTermInstance(termInstance);
        enrollment.setCohort(cohort);
        enrollment.setSemesterNumber(1);
        enrollment.setYearOfStudy(1);
        enrollment.setStatus(com.cms.model.enums.EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        var result = service.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.cohortCode()).isEqualTo("BCA-2024-2027");
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getById(999L))
            .isInstanceOf(com.cms.exception.ResourceNotFoundException.class);
    }

    // ── computeSemesterNumber: YEARLY pattern ─────────────────────────────

    @Test
    void computeSemesterNumber_yearly_oddTerm_yearOne() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createYearlyProgram(1L, "ANM", 4);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);

        assertThat(service.computeSemesterNumber(cohort, ti)).isEqualTo(1);
    }

    @Test
    void computeSemesterNumber_yearly_evenTerm_yearOne_sameAsOdd() {
        // Key invariant: for YEARLY, both ODD and EVEN of the same academic year
        // must return the same position (year 1). SEMESTER would return 2 for EVEN.
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createYearlyProgram(1L, "ANM", 4);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.EVEN);

        assertThat(service.computeSemesterNumber(cohort, ti)).isEqualTo(1);
    }

    @Test
    void computeSemesterNumber_yearly_oddTerm_yearTwo() {
        AcademicYear admissionAY = createAY(1L, "2024-2025");
        AcademicYear secondAY    = createAY(2L, "2025-2026");
        Program program = createYearlyProgram(1L, "ANM", 4);
        Cohort cohort = createCohort(1L, program, admissionAY);
        TermInstance ti = createTermInstance(2L, secondAY, TermType.ODD);

        assertThat(service.computeSemesterNumber(cohort, ti)).isEqualTo(2);
    }

    @Test
    void computeSemesterNumber_yearly_evenTerm_yearTwo_sameAsOdd() {
        AcademicYear admissionAY = createAY(1L, "2024-2025");
        AcademicYear secondAY    = createAY(2L, "2025-2026");
        Program program = createYearlyProgram(1L, "ANM", 4);
        Cohort cohort = createCohort(1L, program, admissionAY);
        TermInstance ti = createTermInstance(2L, secondAY, TermType.EVEN);

        assertThat(service.computeSemesterNumber(cohort, ti)).isEqualTo(2);
    }

    @Test
    void computeSemesterNumber_yearly_lastYear_returnsValue() {
        // A 4-year YEARLY program has totalTerms=4; year 4 must succeed.
        AcademicYear admissionAY = createAY(1L, "2024-2025");
        AcademicYear lastAY      = createAY(4L, "2027-2028");
        Program program = createYearlyProgram(1L, "ANM", 4);
        Cohort cohort = createCohort(1L, program, admissionAY);
        TermInstance ti = createTermInstance(4L, lastAY, TermType.ODD);

        assertThat(service.computeSemesterNumber(cohort, ti)).isEqualTo(4);
    }

    @Test
    void computeSemesterNumber_yearly_outOfRange_returnsNull() {
        // Year after graduation: k = durationYears → position = 5 > totalTerms(4) → null
        AcademicYear admissionAY  = createAY(1L, "2024-2025");
        AcademicYear afterGradAY  = createAY(5L, "2028-2029");
        Program program = createYearlyProgram(1L, "ANM", 4);
        Cohort cohort = createCohort(1L, program, admissionAY);
        TermInstance ti = createTermInstance(5L, afterGradAY, TermType.ODD);

        assertThat(service.computeSemesterNumber(cohort, ti)).isNull();
    }

    @Test
    void computeSemesterNumber_yearly_beforeAdmission_returnsNull() {
        // k < 0: term is in an earlier year than admission → null
        AcademicYear admissionAY = createAY(2L, "2025-2026");
        AcademicYear earlierAY   = createAY(1L, "2024-2025");
        Program program = createYearlyProgram(1L, "ANM", 4);
        Cohort cohort = createCohort(1L, program, admissionAY);
        TermInstance ti = createTermInstance(1L, earlierAY, TermType.ODD);

        assertThat(service.computeSemesterNumber(cohort, ti)).isNull();
    }

    // ── generateEnrollmentsForTermInstance: yearOfStudy verification ──────

    @Test
    void generateEnrollmentsForTermInstance_semester_setsCorrectYearOfStudy() {
        // SEMESTER ODD year 1: semesterNumber=1, yearOfStudy=ceil(1/2)=1
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(studentRepository.findByCohortIdAndStatus(1L, StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentRepository.findByStudentIdAndTermInstanceId(1L, 1L)).thenReturn(Optional.empty());

        ArgumentCaptor<StudentTermEnrollment> captor = ArgumentCaptor.forClass(StudentTermEnrollment.class);
        when(enrollmentRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.generateEnrollmentsForTermInstance(1L);

        StudentTermEnrollment saved = captor.getValue();
        assertThat(saved.getSemesterNumber()).isEqualTo(1);
        assertThat(saved.getYearOfStudy()).isEqualTo(1);
    }

    @Test
    void generateEnrollmentsForTermInstance_yearly_oddTerm_setsYearOfStudyEqualToSemesterNumber() {
        // YEARLY ODD year 1: semesterNumber=1, yearOfStudy must equal semesterNumber (not ceil formula)
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createYearlyProgram(1L, "ANM", 4);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(studentRepository.findByCohortIdAndStatus(1L, StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentRepository.findByStudentIdAndTermInstanceId(1L, 1L)).thenReturn(Optional.empty());

        ArgumentCaptor<StudentTermEnrollment> captor = ArgumentCaptor.forClass(StudentTermEnrollment.class);
        when(enrollmentRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.generateEnrollmentsForTermInstance(1L);

        StudentTermEnrollment saved = captor.getValue();
        assertThat(saved.getSemesterNumber()).isEqualTo(1);
        assertThat(saved.getYearOfStudy()).isEqualTo(1);
    }

    @Test
    void generateEnrollmentsForTermInstance_yearly_evenTerm_enrollsWithSamePositionAsOddTerm() {
        // YEARLY EVEN year 1: enrollment IS created (fee demand skips EVEN, but enrollment does not);
        // semesterNumber and yearOfStudy must both be 1 — same as ODD term of the same year.
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createYearlyProgram(1L, "ANM", 4);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.EVEN);
        Student student = createStudent(1L, program, cohort);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(studentRepository.findByCohortIdAndStatus(1L, StudentStatus.ACTIVE)).thenReturn(List.of(student));
        when(enrollmentRepository.findByStudentIdAndTermInstanceId(1L, 1L)).thenReturn(Optional.empty());

        ArgumentCaptor<StudentTermEnrollment> captor = ArgumentCaptor.forClass(StudentTermEnrollment.class);
        when(enrollmentRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.generateEnrollmentsForTermInstance(1L);

        assertThat(count).isEqualTo(1);
        StudentTermEnrollment saved = captor.getValue();
        assertThat(saved.getSemesterNumber()).isEqualTo(1);
        assertThat(saved.getYearOfStudy()).isEqualTo(1);
    }
}
