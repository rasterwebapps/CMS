package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.cms.model.enums.RegistrationStatus;
import com.cms.model.enums.ResultStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.ExamEventRepository;
import com.cms.repository.SemesterResultRepository;
import com.cms.repository.StudentMarkRepository;
import com.cms.repository.StudentTermEnrollmentRepository;

@ExtendWith(MockitoExtension.class)
class ResultReportServiceImplTest {

    @Mock
    private SemesterResultRepository semesterResultRepository;
    @Mock
    private StudentMarkRepository studentMarkRepository;
    @Mock
    private StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    @Mock
    private ExamEventRepository examEventRepository;

    private ResultReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ResultReportServiceImpl(semesterResultRepository, studentMarkRepository,
            studentTermEnrollmentRepository, examEventRepository);
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

    private ExamSession createSession(Long id, TermInstance ti) {
        ExamSession s = new ExamSession(ti, ExamSessionType.FINAL, ExamSessionStatus.LOCKED,
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

    private ExamEvent createEvent(Long id, ExamSession session, CourseOffering offering) {
        ExamEvent e = new ExamEvent(session, offering, LocalDate.of(2024, 10, 10),
            new BigDecimal("100"), new BigDecimal("40"));
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
        Student s = new Student("ROLL001", "Student", "One", "s" + id + "@test.com",
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

    @Test
    void getResultSheet_success() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration reg = createRegistration(1L, enrollment, offering);

        StudentMark mark = new StudentMark(event, reg, MarkStatus.PRESENT, new BigDecimal("80"), null);
        mark.setId(1L);

        SemesterResult result = new SemesterResult(enrollment, new BigDecimal("100"),
            new BigDecimal("80"), new BigDecimal("80.00"), ResultStatus.PASS);
        result.setId(1L);

        when(studentTermEnrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(semesterResultRepository.findByStudentTermEnrollment_Id(1L)).thenReturn(Optional.of(result));
        when(studentMarkRepository.findByCourseRegistration_StudentTermEnrollment_Id(1L))
            .thenReturn(List.of(mark));

        var sheet = service.getResultSheet(1L);

        assertThat(sheet.studentName()).isEqualTo("Student One");
        assertThat(sheet.rollNumber()).isEqualTo("ROLL001");
        assertThat(sheet.subjectMarks()).hasSize(1);
        assertThat(sheet.resultStatus()).isEqualTo(ResultStatus.PASS);
    }

    @Test
    void getResultSheet_enrollmentNotFound_throws() {
        when(studentTermEnrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getResultSheet(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getResultSheet_resultNotFound_throws() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);

        when(studentTermEnrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(semesterResultRepository.findByStudentTermEnrollment_Id(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getResultSheet(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSummaryByTermInstance_returnsSummary() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        SemesterResult r1 = new SemesterResult(enrollment, new BigDecimal("100"),
            new BigDecimal("80"), new BigDecimal("80.00"), ResultStatus.PASS);
        r1.setId(1L);

        when(semesterResultRepository.findByStudentTermEnrollment_TermInstance_Id(1L))
            .thenReturn(List.of(r1));

        var summaries = service.getSummaryByTermInstance(1L);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).totalStudents()).isEqualTo(1);
        assertThat(summaries.get(0).passCount()).isEqualTo(1);
        assertThat(summaries.get(0).failCount()).isEqualTo(0);
    }

    @Test
    void getCourseStatsByTermInstance_returnsStats() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration reg = createRegistration(1L, enrollment, offering);

        StudentMark mark = new StudentMark(event, reg, MarkStatus.PRESENT, new BigDecimal("75"), null);
        mark.setId(1L);

        when(examEventRepository.findByExamSession_TermInstance_Id(1L)).thenReturn(List.of(event));
        when(studentMarkRepository.findByExamEvent_Id(1L)).thenReturn(List.of(mark));

        var stats = service.getCourseStatsByTermInstance(1L);

        assertThat(stats).hasSize(1);
        assertThat(stats.get(0).totalStudents()).isEqualTo(1);
        assertThat(stats.get(0).presentCount()).isEqualTo(1);
        assertThat(stats.get(0).subjectName()).isEqualTo("Mathematics");
    }
}
