package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.cms.dto.StudentMarkRequest;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.CourseOffering;
import com.cms.model.CourseRegistration;
import com.cms.model.ExamEvent;
import com.cms.model.ExamSession;
import com.cms.model.Program;
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
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.ExamEventRepository;
import com.cms.repository.StudentMarkRepository;

@ExtendWith(MockitoExtension.class)
class StudentMarkServiceImplTest {

    @Mock
    private StudentMarkRepository studentMarkRepository;
    @Mock
    private ExamEventRepository examEventRepository;
    @Mock
    private CourseRegistrationRepository courseRegistrationRepository;

    private StudentMarkServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentMarkServiceImpl(studentMarkRepository, examEventRepository, courseRegistrationRepository);
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
        ExamSession s = new ExamSession(ti, ExamSessionType.INTERNAL1, status,
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

    private Student createStudent(Long id, Program program) {
        Student s = new Student("ROLL00" + id, "Student", "One", "s" + id + "@test.com",
            program, 1, LocalDate.of(2024, 6, 1), StudentStatus.ACTIVE);
        s.setId(id);
        return s;
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
    void upsert_present_success() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration registration = createRegistration(1L, enrollment, offering);

        StudentMarkRequest request = new StudentMarkRequest(1L, 1L, MarkStatus.PRESENT,
            new BigDecimal("75"), null);

        when(examEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(studentMarkRepository.findByExamEvent_IdAndCourseRegistration_Id(1L, 1L))
            .thenReturn(Optional.empty());
        when(studentMarkRepository.save(any(StudentMark.class))).thenAnswer(inv -> {
            StudentMark m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        var result = service.upsert(request);

        assertThat(result.markStatus()).isEqualTo(MarkStatus.PRESENT);
        assertThat(result.marksObtained()).isEqualByComparingTo(new BigDecimal("75"));
        verify(studentMarkRepository).save(any(StudentMark.class));
    }

    @Test
    void upsert_absent_setsZeroMarks() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration registration = createRegistration(1L, enrollment, offering);

        StudentMarkRequest request = new StudentMarkRequest(1L, 1L, MarkStatus.ABSENT, null, null);

        when(examEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(studentMarkRepository.findByExamEvent_IdAndCourseRegistration_Id(1L, 1L))
            .thenReturn(Optional.empty());
        when(studentMarkRepository.save(any(StudentMark.class))).thenAnswer(inv -> {
            StudentMark m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        var result = service.upsert(request);

        assertThat(result.marksObtained()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void upsert_lockedSession_throws() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.LOCKED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);

        StudentMarkRequest request = new StudentMarkRequest(1L, 1L, MarkStatus.PRESENT,
            new BigDecimal("50"), null);
        when(examEventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.upsert(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("LOCKED");
    }

    @Test
    void upsert_marksExceedMax_throws() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration registration = createRegistration(1L, enrollment, offering);

        StudentMarkRequest request = new StudentMarkRequest(1L, 1L, MarkStatus.PRESENT,
            new BigDecimal("150"), null);
        when(examEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(studentMarkRepository.findByExamEvent_IdAndCourseRegistration_Id(1L, 1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(request))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upsert_presentWithNullMarks_throws() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration registration = createRegistration(1L, enrollment, offering);

        StudentMarkRequest request = new StudentMarkRequest(1L, 1L, MarkStatus.PRESENT, null, null);
        when(examEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(studentMarkRepository.findByExamEvent_IdAndCourseRegistration_Id(1L, 1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(request))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getByExamEvent_returnsList() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration registration = createRegistration(1L, enrollment, offering);
        StudentMark mark = new StudentMark(event, registration, MarkStatus.PRESENT,
            new BigDecimal("80"), null);
        mark.setId(1L);

        when(studentMarkRepository.findByExamEvent_Id(1L)).thenReturn(List.of(mark));

        var results = service.getByExamEvent(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).studentName()).isEqualTo("Student One");
    }

    @Test
    void getById_notFound_throws() {
        when(studentMarkRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByEnrollment_returnsList() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration registration = createRegistration(1L, enrollment, offering);
        StudentMark mark = new StudentMark(event, registration, MarkStatus.ABSENT, BigDecimal.ZERO, null);
        mark.setId(1L);

        when(studentMarkRepository.findByCourseRegistration_StudentTermEnrollment_Id(1L))
            .thenReturn(List.of(mark));

        var results = service.getByEnrollment(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).markStatus()).isEqualTo(MarkStatus.ABSENT);
    }

    @Test
    void upsert_negativeMarks_throws() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration registration = createRegistration(1L, enrollment, offering);

        StudentMarkRequest request = new StudentMarkRequest(1L, 1L, MarkStatus.PRESENT,
            new BigDecimal("-5"), null);
        when(examEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(studentMarkRepository.findByExamEvent_IdAndCourseRegistration_Id(1L, 1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("negative");
    }

    @Test
    void upsert_malpractice_setsZeroMarks() {
        AcademicYear ay = createAY(1L);
        TermInstance ti = createTermInstance(1L, ay);
        ExamSession session = createSession(1L, ti, ExamSessionStatus.PUBLISHED);
        Subject subject = createSubject(1L);
        CourseOffering offering = createOffering(1L, ti, subject);
        ExamEvent event = createEvent(1L, session, offering);
        Program program = createProgram(1L);
        Cohort cohort = createCohort(1L, program, ay);
        Student student = createStudent(1L, program);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort);
        CourseRegistration registration = createRegistration(1L, enrollment, offering);

        StudentMarkRequest request = new StudentMarkRequest(1L, 1L, MarkStatus.MALPRACTICE, null, "Caught cheating");

        when(examEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(registration));
        when(studentMarkRepository.findByExamEvent_IdAndCourseRegistration_Id(1L, 1L))
            .thenReturn(Optional.empty());
        when(studentMarkRepository.save(any(StudentMark.class))).thenAnswer(inv -> {
            StudentMark m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        var result = service.upsert(request);

        assertThat(result.markStatus()).isEqualTo(MarkStatus.MALPRACTICE);
        assertThat(result.marksObtained()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
