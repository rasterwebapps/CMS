package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.CourseOffering;
import com.cms.model.CourseRegistration;
import com.cms.model.ExamEvent;
import com.cms.model.ExamSession;
import com.cms.model.Program;
import com.cms.model.SemesterResult;
import com.cms.model.Student;
import com.cms.model.StudentMark;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.ExamSessionStatus;
import com.cms.model.enums.ExamSessionType;
import com.cms.model.enums.MarkStatus;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.ResultStatus;
import com.cms.model.enums.RegistrationStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.ExamSessionRepository;
import com.cms.repository.SemesterResultRepository;
import com.cms.repository.StudentMarkRepository;
import com.cms.repository.StudentTermEnrollmentRepository;

@ExtendWith(MockitoExtension.class)
class SemesterResultServiceImplTest {

    @Mock
    private SemesterResultRepository semesterResultRepository;
    @Mock
    private StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    @Mock
    private StudentMarkRepository studentMarkRepository;
    @Mock
    private ExamSessionRepository examSessionRepository;

    private SemesterResultServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SemesterResultServiceImpl(semesterResultRepository,
            studentTermEnrollmentRepository, studentMarkRepository, examSessionRepository);
    }

    private AcademicYear createAY(Long id) {
        AcademicYear ay = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(id);
        return ay;
    }

    private TermInstance createTermInstance(Long id, AcademicYear ay) {
        TermInstance ti = new TermInstance(ay, TermType.ODD, LocalDate.of(2024, 6, 1),
            LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN);
        ti.setId(id);
        return ti;
    }

    private ExamSession createSession(Long id, TermInstance ti, ExamSessionStatus status) {
        ExamSession s = new ExamSession(ti, ExamSessionType.FINAL, status,
            LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 30));
        s.setId(id);
        return s;
    }

    private Subject createSubject(Long id) {
        Subject s = new Subject();
        s.setId(id);
        s.setName("Mathematics");
        s.setCode("MATH101");
        return s;
    }

    private CourseOffering createOffering(Long id, TermInstance ti, Subject subject) {
        CourseOffering o = new CourseOffering();
        o.setId(id);
        o.setTermInstance(ti);
        o.setSubject(subject);
        o.setSemesterNumber(1);
        o.setIsActive(true);
        o.setCreatedAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        return o;
    }

    private ExamEvent createEvent(Long id, ExamSession session, CourseOffering offering, BigDecimal maxMarks) {
        ExamEvent e = new ExamEvent(session, offering, LocalDate.of(2024, 10, 10),
            maxMarks, maxMarks.multiply(new BigDecimal("0.4")));
        e.setId(id);
        return e;
    }

    private Program createProgram(Long id) {
        Program p = new Program("BCA Program", "BCA", 3, ProgramStatus.ACTIVE);
        p.setId(id);
        return p;
    }

    private Cohort createCohort(Long id, Program program, AcademicYear ay) {
        Cohort c = new Cohort();
        c.setId(id);
        c.setProgram(program);
        c.setAdmissionAcademicYear(ay);
        c.setCohortCode("BCA-2024-2027");
        c.setDisplayName("BCA (2024-2027)");
        c.setStatus(CohortStatus.ACTIVE);
        return c;
    }

    private Student createStudent(Long id, Program program) {
        Student s = new Student("ROLL00" + id, "Student", "One", "s" + id + "@test.com",
            program, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        s.setId(id);
        return s;
    }

    private StudentTermEnrollment createEnrollment(Long id, Student student, TermInstance ti, Cohort cohort) {
        StudentTermEnrollment e = new StudentTermEnrollment();
        e.setId(id);
        e.setStudent(student);
        e.setTermInstance(ti);
        e.setCohort(cohort);
        e.setSemesterNumber(1);
        e.setYearOfStudy(1);
        e.setStatus(EnrollmentStatus.ENROLLED);
        return e;
    }

    private CourseRegistration createRegistration(Long id, StudentTermEnrollment enrollment, CourseOffering offering) {
        CourseRegistration r = new CourseRegistration();
        r.setId(id);
        r.setStudentTermEnrollment(enrollment);
        r.setCourseOffering(offering);
        r.setStatus(RegistrationStatus.REGISTERED);
        return r;
    }

    private StudentMark createMark(Long id, ExamEvent event, CourseRegistration reg,
                                    MarkStatus status, BigDecimal marks) {
        StudentMark m = new StudentMark(event, reg, status, marks, null);
        m.setId(id);
        return m;
    }

    @Test
    void computeForEnrollment_pass_success() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.LOCKED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering, new BigDecimal("100"));
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration reg = createRegistration(1L, enrollment, offering);
        StudentMark mark = createMark(1L, event, reg, MarkStatus.PRESENT, new BigDecimal("80"));

        when(studentTermEnrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(studentMarkRepository.findByCourseRegistration_StudentTermEnrollment_Id(1L))
            .thenReturn(List.of(mark));
        when(semesterResultRepository.findByStudentTermEnrollment_Id(1L)).thenReturn(Optional.empty());
        when(semesterResultRepository.save(any(SemesterResult.class))).thenAnswer(inv -> {
            SemesterResult r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        var result = service.computeForEnrollment(1L);

        assertThat(result.resultStatus()).isEqualTo(ResultStatus.PASS);
        assertThat(result.percentage()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    void computeForEnrollment_fail_belowThreshold() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.LOCKED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering, new BigDecimal("100"));
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration reg = createRegistration(1L, enrollment, offering);
        StudentMark mark = createMark(1L, event, reg, MarkStatus.PRESENT, new BigDecimal("30"));

        when(studentTermEnrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(studentMarkRepository.findByCourseRegistration_StudentTermEnrollment_Id(1L))
            .thenReturn(List.of(mark));
        when(semesterResultRepository.findByStudentTermEnrollment_Id(1L)).thenReturn(Optional.empty());
        when(semesterResultRepository.save(any(SemesterResult.class))).thenAnswer(inv -> {
            SemesterResult r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        var result = service.computeForEnrollment(1L);

        assertThat(result.resultStatus()).isEqualTo(ResultStatus.FAIL);
    }

    @Test
    void computeForEnrollment_lockedResult_throws() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        SemesterResult locked = new SemesterResult(enrollment, new BigDecimal("100"),
            new BigDecimal("80"), new BigDecimal("80"), ResultStatus.PASS);
        locked.setId(1L);
        locked.setIsLocked(true);

        when(studentTermEnrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(studentMarkRepository.findByCourseRegistration_StudentTermEnrollment_Id(1L))
            .thenReturn(List.of());
        when(semesterResultRepository.findByStudentTermEnrollment_Id(1L)).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.computeForEnrollment(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("locked");
    }

    @Test
    void computeResultsForTermInstance_allSessionsNotLocked_throws() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);

        when(examSessionRepository.findByTermInstance_Id(1L)).thenReturn(List.of(session));

        assertThatThrownBy(() -> service.computeResultsForTermInstance(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("LOCKED");
    }

    @Test
    void lockResult_success() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        SemesterResult result = new SemesterResult(enrollment, new BigDecimal("100"),
            new BigDecimal("80"), new BigDecimal("80"), ResultStatus.PASS);
        result.setId(1L);
        result.setIsLocked(false);

        when(semesterResultRepository.findById(1L)).thenReturn(Optional.of(result));
        when(semesterResultRepository.save(any(SemesterResult.class))).thenReturn(result);

        var dto = service.lockResult(1L);

        assertThat(dto.isLocked()).isTrue();
    }

    @Test
    void getByEnrollment_notFound_throws() {
        when(semesterResultRepository.findByStudentTermEnrollment_Id(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByEnrollment(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByTermInstance_returnsList() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        SemesterResult result = new SemesterResult(enrollment, new BigDecimal("100"),
            new BigDecimal("80"), new BigDecimal("80"), ResultStatus.PASS);
        result.setId(1L);

        when(semesterResultRepository.findByStudentTermEnrollment_TermInstance_Id(1L))
            .thenReturn(List.of(result));

        var results = service.getByTermInstance(1L);

        assertThat(results).hasSize(1);
    }

    @Test
    void getByStudent_returnsList() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        SemesterResult result = new SemesterResult(enrollment, new BigDecimal("100"),
            new BigDecimal("80"), new BigDecimal("80"), ResultStatus.PASS);
        result.setId(1L);

        when(semesterResultRepository.findByStudentTermEnrollment_Student_Id(1L))
            .thenReturn(List.of(result));

        var results = service.getByStudent(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).resultStatus()).isEqualTo(ResultStatus.PASS);
    }

    @Test
    void computeResultsForTermInstance_computesForUnlockedEnrollments() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.LOCKED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering, new BigDecimal("100"));
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration reg = createRegistration(1L, enrollment, offering);
        StudentMark mark = createMark(1L, event, reg, MarkStatus.PRESENT, new BigDecimal("80"));

        when(examSessionRepository.findByTermInstance_Id(1L)).thenReturn(List.of(session));
        when(studentTermEnrollmentRepository.findByTermInstanceId(1L)).thenReturn(List.of(enrollment));
        when(studentTermEnrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(semesterResultRepository.findByStudentTermEnrollment_Id(1L)).thenReturn(Optional.empty());
        when(studentMarkRepository.findByCourseRegistration_StudentTermEnrollment_Id(1L))
            .thenReturn(List.of(mark));
        when(semesterResultRepository.save(any(SemesterResult.class))).thenAnswer(inv -> {
            SemesterResult r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        service.computeResultsForTermInstance(1L);

        verify(semesterResultRepository).save(any(SemesterResult.class));
    }

    @Test
    void computeResultsForTermInstance_skipsLockedResults() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.LOCKED);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        SemesterResult locked = new SemesterResult(enrollment, new BigDecimal("100"),
            new BigDecimal("80"), new BigDecimal("80"), ResultStatus.PASS);
        locked.setId(1L);
        locked.setIsLocked(true);

        when(examSessionRepository.findByTermInstance_Id(1L)).thenReturn(List.of(session));
        when(studentTermEnrollmentRepository.findByTermInstanceId(1L)).thenReturn(List.of(enrollment));
        when(semesterResultRepository.findByStudentTermEnrollment_Id(1L)).thenReturn(Optional.of(locked));

        service.computeResultsForTermInstance(1L);

        verify(semesterResultRepository, never()).save(any());
    }
}
