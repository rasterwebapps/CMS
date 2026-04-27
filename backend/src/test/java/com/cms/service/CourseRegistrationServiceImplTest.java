package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.CourseRegistrationDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.CourseOffering;
import com.cms.model.CourseRegistration;
import com.cms.model.CurriculumVersion;
import com.cms.model.Program;
import com.cms.model.Student;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.RegistrationStatus;
import com.cms.model.enums.StudentStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceImplTest {

    @Mock
    private CourseRegistrationRepository courseRegistrationRepository;
    @Mock
    private StudentTermEnrollmentRepository enrollmentRepository;
    @Mock
    private CourseOfferingRepository courseOfferingRepository;
    @Mock
    private TermInstanceRepository termInstanceRepository;

    private CourseRegistrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationServiceImpl(
            courseRegistrationRepository, enrollmentRepository,
            courseOfferingRepository, termInstanceRepository);
    }

    private AcademicYear createAY(Long id, String name) {
        AcademicYear ay = new AcademicYear(name, LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(id);
        return ay;
    }

    private Program createProgram(Long id, String code) {
        Program p = new Program(code + " Program", code, 3, ProgramStatus.ACTIVE);
        p.setId(id);
        return p;
    }

    private Cohort createCohort(Long id, Program program, AcademicYear ay) {
        Cohort c = new Cohort();
        c.setId(id);
        c.setProgram(program);
        c.setAdmissionAcademicYear(ay);
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

    private Subject createSubject(Long id, String name, String code) {
        Subject s = new Subject();
        s.setId(id);
        s.setName(name);
        s.setCode(code);
        return s;
    }

    private CurriculumVersion createCV(Long id, Program program, AcademicYear ay) {
        CurriculumVersion cv = new CurriculumVersion(program, "CV-2024", ay, true);
        cv.setId(id);
        return cv;
    }

    private StudentTermEnrollment createEnrollment(Long id, Student student, TermInstance ti,
                                                    Cohort cohort, int semNum) {
        StudentTermEnrollment e = new StudentTermEnrollment();
        e.setId(id);
        e.setStudent(student);
        e.setTermInstance(ti);
        e.setCohort(cohort);
        e.setSemesterNumber(semNum);
        e.setYearOfStudy(1);
        e.setStatus(EnrollmentStatus.ENROLLED);
        return e;
    }

    private CourseOffering createOffering(Long id, TermInstance ti, CurriculumVersion cv,
                                          Subject subject, int semNum) {
        CourseOffering o = new CourseOffering();
        o.setId(id);
        o.setTermInstance(ti);
        o.setCurriculumVersion(cv);
        o.setSubject(subject);
        o.setSemesterNumber(semNum);
        o.setIsActive(true);
        o.setCreatedAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        return o;
    }

    @Test
    void generateRegistrationsForTermInstance_createsRegistrations() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);

        when(termInstanceRepository.existsById(1L)).thenReturn(true);
        when(enrollmentRepository.findByTermInstanceId(1L)).thenReturn(List.of(enrollment));
        when(courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(1L, 1))
            .thenReturn(List.of(offering));
        when(courseRegistrationRepository.findByStudentTermEnrollmentIdAndCourseOfferingId(1L, 1L))
            .thenReturn(Optional.empty());
        when(courseRegistrationRepository.save(any(CourseRegistration.class))).thenAnswer(inv -> {
            CourseRegistration r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        int count = service.generateRegistrationsForTermInstance(1L);

        assertThat(count).isEqualTo(1);
        verify(courseRegistrationRepository).save(any(CourseRegistration.class));
    }

    @Test
    void generateRegistrationsForTermInstance_isIdempotent() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);
        CourseRegistration existing = new CourseRegistration();
        existing.setId(99L);

        when(termInstanceRepository.existsById(1L)).thenReturn(true);
        when(enrollmentRepository.findByTermInstanceId(1L)).thenReturn(List.of(enrollment));
        when(courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(1L, 1))
            .thenReturn(List.of(offering));
        when(courseRegistrationRepository.findByStudentTermEnrollmentIdAndCourseOfferingId(1L, 1L))
            .thenReturn(Optional.of(existing));

        int count = service.generateRegistrationsForTermInstance(1L);

        assertThat(count).isEqualTo(0);
        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void generateRegistrationsForTermInstance_skipsInactiveOfferings() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);
        offering.setIsActive(false); // Inactive

        when(termInstanceRepository.existsById(1L)).thenReturn(true);
        when(enrollmentRepository.findByTermInstanceId(1L)).thenReturn(List.of(enrollment));
        when(courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(1L, 1))
            .thenReturn(List.of(offering));

        int count = service.generateRegistrationsForTermInstance(1L);

        assertThat(count).isEqualTo(0);
        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void generateRegistrationsForTermInstance_throwsWhenTermInstanceNotFound() {
        when(termInstanceRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.generateRegistrationsForTermInstance(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void getRegistrationsByEnrollment_returnsMappedDtos() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);

        CourseRegistration reg = new CourseRegistration();
        reg.setId(1L);
        reg.setStudentTermEnrollment(enrollment);
        reg.setCourseOffering(offering);
        reg.setStatus(RegistrationStatus.REGISTERED);
        reg.setCreatedAt(Instant.now());
        reg.setUpdatedAt(Instant.now());

        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of(reg));

        List<CourseRegistrationDto> dtos = service.getRegistrationsByEnrollment(1L);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).status()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(dtos.get(0).subjectCode()).isEqualTo("MATH101");
    }

    @Test
    void dropRegistration_setsStatusDropped() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);

        CourseRegistration reg = new CourseRegistration();
        reg.setId(1L);
        reg.setStudentTermEnrollment(enrollment);
        reg.setCourseOffering(offering);
        reg.setStatus(RegistrationStatus.REGISTERED);
        reg.setCreatedAt(Instant.now());
        reg.setUpdatedAt(Instant.now());

        when(courseRegistrationRepository.findById(1L)).thenReturn(Optional.of(reg));
        when(courseRegistrationRepository.save(any(CourseRegistration.class))).thenReturn(reg);

        CourseRegistrationDto dto = service.dropRegistration(1L);

        assertThat(dto.status()).isEqualTo(RegistrationStatus.DROPPED);
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(courseRegistrationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }
}
