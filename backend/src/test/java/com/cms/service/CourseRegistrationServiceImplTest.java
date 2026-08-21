package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.cms.dto.CourseRegistrationDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Course;
import com.cms.model.CourseOffering;
import com.cms.model.CourseRegistration;
import com.cms.model.CurriculumElectiveGroup;
import com.cms.model.CurriculumSemesterCourse;
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
import com.cms.model.enums.SubjectType;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.AttendanceRepository;
import com.cms.repository.ClassScheduleRepository;
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
    @Mock
    private ClassScheduleRepository classScheduleRepository;
    @Mock
    private AttendanceRepository attendanceRepository;

    private CourseRegistrationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationServiceImpl(
            courseRegistrationRepository, enrollmentRepository,
            courseOfferingRepository, termInstanceRepository,
            classScheduleRepository, attendanceRepository);
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

    private Course createCourse(Long id, String name, String code, Program program) {
        Course course = new Course(name, code, null, program);
        course.setId(id);
        return course;
    }

    private Cohort createCohort(Long id, Course course, AcademicYear ay) {
        Cohort c = new Cohort();
        c.setId(id);
        c.setCourse(course);
        c.setAdmissionAcademicYear(ay);
        c.setCohortCode(course.getCode() + "-2024-2027");
        c.setDisplayName(course.getName() + " (2024-2027)");
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

    private CurriculumVersion createCV(Long id, Program program, Course course, AcademicYear ay) {
        CurriculumVersion cv = new CurriculumVersion(program, course, "CV-2024", ay, true);
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
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
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
    void generateRegistrationsForTermInstance_excludesOfferingsFromDifferentCourseUnderSameProgram() {
        // Regression test: MSc Nursing (Adult) and (Child) share one Program but — since
        // CurriculumVersion.course is now mandatory — each has its own course-scoped
        // CurriculumVersion rather than one shared program-wide version with row-level
        // exceptions. An Adult student's enrollment must not pick up a Child-only course offering.
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "MSc");
        Course adultCourse = createCourse(1L, "MSc Nursing (Adult)", "MSN-A", program);
        Course childCourse = createCourse(2L, "MSc Nursing (Child)", "MSN-C", program);
        Cohort adultCohort = createCohort(1L, adultCourse, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, adultCohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, adultCohort, 1);

        Subject adultSubject = createSubject(1L, "Advanced Medical Surgical Nursing", "MSNA101");
        Subject childSubject = createSubject(2L, "Advanced Child Health Nursing", "MSNC101");
        CurriculumVersion adultCv = createCV(1L, program, adultCourse, ay);
        CurriculumVersion childCv = createCV(2L, program, childCourse, ay);
        CourseOffering adultOffering = createOffering(1L, ti, adultCv, adultSubject, 1);
        CourseOffering childOffering = createOffering(2L, ti, childCv, childSubject, 1);

        when(termInstanceRepository.existsById(1L)).thenReturn(true);
        when(enrollmentRepository.findByTermInstanceId(1L)).thenReturn(List.of(enrollment));
        when(courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(1L, 1))
            .thenReturn(List.of(adultOffering, childOffering));
        when(courseRegistrationRepository.findByStudentTermEnrollmentIdAndCourseOfferingId(1L, 1L))
            .thenReturn(Optional.empty());
        when(courseRegistrationRepository.save(any(CourseRegistration.class))).thenAnswer(inv -> {
            CourseRegistration r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        int count = service.generateRegistrationsForTermInstance(1L);

        assertThat(count).isEqualTo(1);
        verify(courseRegistrationRepository, never())
            .findByStudentTermEnrollmentIdAndCourseOfferingId(1L, 2L);
    }

    @Test
    void generateRegistrationsForTermInstance_isIdempotent() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
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
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
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
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
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
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
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

    private CurriculumSemesterCourse createElectiveCsc(Long id, CurriculumVersion cv, int semNum,
                                                        Subject subject, CurriculumElectiveGroup group) {
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse(cv, semNum, subject, 1);
        csc.setId(id);
        csc.setIsElective(true);
        csc.setSubjectType(SubjectType.ELECTIVE);
        csc.setElectiveGroup(group);
        return csc;
    }

    @Test
    void generateRegistrationsForTermInstance_excludesElectiveOfferings() {
        // Regression: a curriculum version with zero electives must behave identically to today
        // (covered by the existing tests above, which use offerings with no curriculum mapping at
        // all). This test covers the new behavior: an offering whose mapping IS marked elective
        // must be skipped by bulk-generate, left for assignElectiveChoice() instead.
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Community Health Elective", "ELEC101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumElectiveGroup group = new CurriculumElectiveGroup(cv, 1, "Term 1 Electives", "T1E");
        group.setId(1L);
        CurriculumSemesterCourse csc = createElectiveCsc(1L, cv, 1, subject, group);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);
        offering.setCurriculumSemesterCourse(csc);

        when(termInstanceRepository.existsById(1L)).thenReturn(true);
        when(enrollmentRepository.findByTermInstanceId(1L)).thenReturn(List.of(enrollment));
        when(courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(1L, 1))
            .thenReturn(List.of(offering));

        int count = service.generateRegistrationsForTermInstance(1L);

        assertThat(count).isEqualTo(0);
        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void assignElectiveChoice_rejectsNonElectiveOffering() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1); // no curriculumSemesterCourse

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));

        assertThatThrownBy(() -> service.assignElectiveChoice(1L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a choice-based elective");

        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void assignElectiveChoice_createsRegistrationForValidElective() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Community Health Elective", "ELEC101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumElectiveGroup group = new CurriculumElectiveGroup(cv, 1, "Term 1 Electives", "T1E");
        group.setId(1L);
        CurriculumSemesterCourse csc = createElectiveCsc(1L, cv, 1, subject, group);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);
        offering.setCurriculumSemesterCourse(csc);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of());
        when(courseRegistrationRepository.save(any(CourseRegistration.class))).thenAnswer(inv -> {
            CourseRegistration r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        CourseRegistrationDto dto = service.assignElectiveChoice(1L, 1L);

        assertThat(dto.status()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(dto.courseOfferingId()).isEqualTo(1L);
    }

    @Test
    void assignElectiveChoice_changesToNewOfferingWhenSafe() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subjectA = createSubject(1L, "Community Health Elective", "ELECA");
        Subject subjectB = createSubject(2L, "School Health Elective", "ELECB");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumElectiveGroup group = new CurriculumElectiveGroup(cv, 1, "Term 1 Electives", "T1E");
        group.setId(1L);
        CurriculumSemesterCourse cscA = createElectiveCsc(1L, cv, 1, subjectA, group);
        CurriculumSemesterCourse cscB = createElectiveCsc(2L, cv, 1, subjectB, group);
        CourseOffering offeringA = createOffering(1L, ti, cv, subjectA, 1);
        offeringA.setCurriculumSemesterCourse(cscA);
        CourseOffering offeringB = createOffering(2L, ti, cv, subjectB, 1);
        offeringB.setCurriculumSemesterCourse(cscB);

        CourseRegistration existingChoice = new CourseRegistration();
        existingChoice.setId(50L);
        existingChoice.setStudentTermEnrollment(enrollment);
        existingChoice.setCourseOffering(offeringA);
        existingChoice.setStatus(RegistrationStatus.REGISTERED);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(courseOfferingRepository.findById(2L)).thenReturn(Optional.of(offeringB));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of(existingChoice));
        when(courseRegistrationRepository.save(any(CourseRegistration.class))).thenAnswer(inv -> {
            CourseRegistration r = inv.getArgument(0);
            r.setId(51L);
            return r;
        });

        CourseRegistrationDto dto = service.assignElectiveChoice(1L, 2L);

        assertThat(dto.courseOfferingId()).isEqualTo(2L);
        assertThat(existingChoice.getStatus()).isEqualTo(RegistrationStatus.DROPPED);
        verify(courseRegistrationRepository).saveAndFlush(existingChoice);
    }

    @Test
    void assignElectiveChoice_reactivatesPreviouslyDroppedRegistrationInsteadOfInserting() {
        // Regression for a real production bug: A -> B -> back to A left a DROPPED row for A
        // already in the table. The old code always built a brand-new CourseRegistration for the
        // target offering, which collided with V71's unscoped UNIQUE(enrollment, offering)
        // constraint -- surfacing as a generic "record already exists" 409 to the admin. Reusing
        // the dropped row instead of inserting a new one is what fixes it.
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subjectA = createSubject(1L, "Community Health Elective", "ELECA");
        Subject subjectB = createSubject(2L, "School Health Elective", "ELECB");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumElectiveGroup group = new CurriculumElectiveGroup(cv, 1, "Term 1 Electives", "T1E");
        group.setId(1L);
        CurriculumSemesterCourse cscA = createElectiveCsc(1L, cv, 1, subjectA, group);
        CurriculumSemesterCourse cscB = createElectiveCsc(2L, cv, 1, subjectB, group);
        CourseOffering offeringA = createOffering(1L, ti, cv, subjectA, 1);
        offeringA.setCurriculumSemesterCourse(cscA);
        CourseOffering offeringB = createOffering(2L, ti, cv, subjectB, 1);
        offeringB.setCurriculumSemesterCourse(cscB);

        CourseRegistration currentChoiceB = new CourseRegistration();
        currentChoiceB.setId(60L);
        currentChoiceB.setStudentTermEnrollment(enrollment);
        currentChoiceB.setCourseOffering(offeringB);
        currentChoiceB.setStatus(RegistrationStatus.REGISTERED);

        // The student's original A registration, already DROPPED from an earlier change to B.
        CourseRegistration droppedA = new CourseRegistration();
        droppedA.setId(50L);
        droppedA.setStudentTermEnrollment(enrollment);
        droppedA.setCourseOffering(offeringA);
        droppedA.setStatus(RegistrationStatus.DROPPED);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offeringA));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L))
            .thenReturn(List.of(currentChoiceB, droppedA));
        when(courseRegistrationRepository.findByStudentTermEnrollmentIdAndCourseOfferingId(1L, 1L))
            .thenReturn(Optional.of(droppedA));
        when(courseRegistrationRepository.save(any(CourseRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseRegistrationDto dto = service.assignElectiveChoice(1L, 1L);

        assertThat(dto.id()).isEqualTo(50L);
        assertThat(dto.courseOfferingId()).isEqualTo(1L);
        assertThat(droppedA.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        ArgumentCaptor<CourseRegistration> captor = ArgumentCaptor.forClass(CourseRegistration.class);
        verify(courseRegistrationRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(50L);
    }

    @Test
    void assignElectiveChoice_blocksChangeWhenTermLocked() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        ti.setStatus(TermInstanceStatus.LOCKED);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subjectA = createSubject(1L, "Community Health Elective", "ELECA");
        Subject subjectB = createSubject(2L, "School Health Elective", "ELECB");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumElectiveGroup group = new CurriculumElectiveGroup(cv, 1, "Term 1 Electives", "T1E");
        group.setId(1L);
        CurriculumSemesterCourse cscB = createElectiveCsc(2L, cv, 1, subjectB, group);
        CourseOffering offeringB = createOffering(2L, ti, cv, subjectB, 1);
        offeringB.setCurriculumSemesterCourse(cscB);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(courseOfferingRepository.findById(2L)).thenReturn(Optional.of(offeringB));

        assertThatThrownBy(() -> service.assignElectiveChoice(1L, 2L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("term is locked");

        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void assignElectiveChoice_blocksChangeWhenGroupAlreadyScheduled() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subjectA = createSubject(1L, "Community Health Elective", "ELECA");
        Subject subjectB = createSubject(2L, "School Health Elective", "ELECB");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumElectiveGroup group = new CurriculumElectiveGroup(cv, 1, "Term 1 Electives", "T1E");
        group.setId(1L);
        CurriculumSemesterCourse cscA = createElectiveCsc(1L, cv, 1, subjectA, group);
        CurriculumSemesterCourse cscB = createElectiveCsc(2L, cv, 1, subjectB, group);
        CourseOffering offeringA = createOffering(1L, ti, cv, subjectA, 1);
        offeringA.setCurriculumSemesterCourse(cscA);
        CourseOffering offeringB = createOffering(2L, ti, cv, subjectB, 1);
        offeringB.setCurriculumSemesterCourse(cscB);

        CourseRegistration existingChoice = new CourseRegistration();
        existingChoice.setId(50L);
        existingChoice.setStudentTermEnrollment(enrollment);
        existingChoice.setCourseOffering(offeringA);
        existingChoice.setStatus(RegistrationStatus.REGISTERED);

        com.cms.model.ClassSchedule placedSession = new com.cms.model.ClassSchedule();

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(courseOfferingRepository.findById(2L)).thenReturn(Optional.of(offeringB));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of(existingChoice));
        when(courseOfferingRepository.findByTermInstanceIdAndCurriculumSemesterCourse_ElectiveGroupId(1L, 1L))
            .thenReturn(List.of(offeringA, offeringB));
        when(classScheduleRepository.findByTermInstanceIdAndCourseOfferingIdIn(1L, List.of(1L, 2L)))
            .thenReturn(List.of(placedSession));

        assertThatThrownBy(() -> service.assignElectiveChoice(1L, 2L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Skeleton Builder");

        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void assignElectiveChoice_blocksChangeWhenAttendanceAlreadyRecorded() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subjectA = createSubject(1L, "Community Health Elective", "ELECA");
        Subject subjectB = createSubject(2L, "School Health Elective", "ELECB");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumElectiveGroup group = new CurriculumElectiveGroup(cv, 1, "Term 1 Electives", "T1E");
        group.setId(1L);
        CurriculumSemesterCourse cscA = createElectiveCsc(1L, cv, 1, subjectA, group);
        CurriculumSemesterCourse cscB = createElectiveCsc(2L, cv, 1, subjectB, group);
        CourseOffering offeringA = createOffering(1L, ti, cv, subjectA, 1);
        offeringA.setCurriculumSemesterCourse(cscA);
        CourseOffering offeringB = createOffering(2L, ti, cv, subjectB, 1);
        offeringB.setCurriculumSemesterCourse(cscB);

        CourseRegistration existingChoice = new CourseRegistration();
        existingChoice.setId(50L);
        existingChoice.setStudentTermEnrollment(enrollment);
        existingChoice.setCourseOffering(offeringA);
        existingChoice.setStatus(RegistrationStatus.REGISTERED);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(courseOfferingRepository.findById(2L)).thenReturn(Optional.of(offeringB));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of(existingChoice));
        when(attendanceRepository.existsByStudentIdAndSubjectId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.assignElectiveChoice(1L, 2L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("attendance");

        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void assignElectiveChoice_isIdempotentForSameOffering() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);
        Subject subject = createSubject(1L, "Community Health Elective", "ELEC101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumElectiveGroup group = new CurriculumElectiveGroup(cv, 1, "Term 1 Electives", "T1E");
        group.setId(1L);
        CurriculumSemesterCourse csc = createElectiveCsc(1L, cv, 1, subject, group);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);
        offering.setCurriculumSemesterCourse(csc);

        CourseRegistration existingChoice = new CourseRegistration();
        existingChoice.setId(50L);
        existingChoice.setStudentTermEnrollment(enrollment);
        existingChoice.setCourseOffering(offering);
        existingChoice.setStatus(RegistrationStatus.REGISTERED);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of(existingChoice));

        CourseRegistrationDto dto = service.assignElectiveChoice(1L, 1L);

        assertThat(dto.id()).isEqualTo(50L);
        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void bulkAssignElectiveChoice_rejectsOfferingNotInGroup() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        CurriculumVersion cv = createCV(1L, program, course, ay);
        Subject subject = createSubject(1L, "Math", "MATH101");
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1); // no curriculumSemesterCourse

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));

        assertThatThrownBy(() -> service.bulkAssignElectiveChoice(1L, 99L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to elective group");

        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void bulkAssignElectiveChoice_throwsWhenTermLocked() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        CurriculumVersion cv = createCV(1L, program, course, ay);
        Subject subject = createSubject(1L, "Math", "MATH101");
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        ti.setStatus(TermInstanceStatus.LOCKED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));

        assertThatThrownBy(() -> service.bulkAssignElectiveChoice(1L, 1L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("term is locked");

        verify(courseRegistrationRepository, never()).save(any());
    }

    @Test
    void bulkAssignElectiveChoice_assignsUnassignedAndOverwritesConflicting() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student studentA = createStudent(1L, program, cohort);
        Student studentB = createStudent(2L, program, cohort);
        StudentTermEnrollment enrollmentA = createEnrollment(1L, studentA, ti, cohort, 1);
        StudentTermEnrollment enrollmentB = createEnrollment(2L, studentB, ti, cohort, 1);

        Subject subjectChosen = createSubject(1L, "Community Health Elective", "ELECA");
        Subject subjectOther = createSubject(2L, "School Health Elective", "ELECB");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumElectiveGroup group = new CurriculumElectiveGroup(cv, 1, "Term 1 Electives", "T1E");
        group.setId(1L);
        CurriculumSemesterCourse cscChosen = createElectiveCsc(1L, cv, 1, subjectChosen, group);
        CurriculumSemesterCourse cscOther = createElectiveCsc(2L, cv, 1, subjectOther, group);
        CourseOffering chosenOffering = createOffering(1L, ti, cv, subjectChosen, 1);
        chosenOffering.setCurriculumSemesterCourse(cscChosen);
        CourseOffering otherOffering = createOffering(2L, ti, cv, subjectOther, 1);
        otherOffering.setCurriculumSemesterCourse(cscOther);

        // studentB already has a choice in this group, but for the *other* offering -- bulk-assign
        // must drop it and reassign to the chosen offering ("overwrite everyone").
        CourseRegistration existingForB = new CourseRegistration();
        existingForB.setId(50L);
        existingForB.setStudentTermEnrollment(enrollmentB);
        existingForB.setCourseOffering(otherOffering);
        existingForB.setStatus(RegistrationStatus.REGISTERED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(chosenOffering));
        when(enrollmentRepository.findByTermInstanceIdAndSemesterNumberAndCohortCourseIdAndStatus(
                1L, 1, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(List.of(enrollmentA, enrollmentB));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of());
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(2L)).thenReturn(List.of(existingForB));
        when(courseRegistrationRepository.save(any(CourseRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.bulkAssignElectiveChoice(1L, 1L, 1L);

        assertThat(response.eligibleStudentCount()).isEqualTo(2);
        assertThat(response.assignedCount()).isEqualTo(2);
        assertThat(response.blockedCount()).isEqualTo(0);
        assertThat(existingForB.getStatus()).isEqualTo(RegistrationStatus.DROPPED);
        verify(courseRegistrationRepository, times(2))
            .save(argThat(r ->
                r.getStatus() == RegistrationStatus.REGISTERED && r.getCourseOffering().getId().equals(1L)));
    }

    @Test
    void bulkAssignElectiveChoice_skipsAndCountsBlockedWhenAttendanceAlreadyRecorded() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student studentA = createStudent(1L, program, cohort);
        Student studentB = createStudent(2L, program, cohort);
        StudentTermEnrollment enrollmentA = createEnrollment(1L, studentA, ti, cohort, 1);
        StudentTermEnrollment enrollmentB = createEnrollment(2L, studentB, ti, cohort, 1);

        Subject subjectChosen = createSubject(1L, "Community Health Elective", "ELECA");
        Subject subjectOther = createSubject(2L, "School Health Elective", "ELECB");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumElectiveGroup group = new CurriculumElectiveGroup(cv, 1, "Term 1 Electives", "T1E");
        group.setId(1L);
        CurriculumSemesterCourse cscChosen = createElectiveCsc(1L, cv, 1, subjectChosen, group);
        CurriculumSemesterCourse cscOther = createElectiveCsc(2L, cv, 1, subjectOther, group);
        CourseOffering chosenOffering = createOffering(1L, ti, cv, subjectChosen, 1);
        chosenOffering.setCurriculumSemesterCourse(cscChosen);
        CourseOffering otherOffering = createOffering(2L, ti, cv, subjectOther, 1);
        otherOffering.setCurriculumSemesterCourse(cscOther);

        // studentB already attended sessions under their current (other) offering -- bulk-assign
        // must skip them (leave their choice untouched) rather than fail the whole run, and report
        // them via blockedCount so the admin can see why the counts don't add up.
        CourseRegistration existingForB = new CourseRegistration();
        existingForB.setId(50L);
        existingForB.setStudentTermEnrollment(enrollmentB);
        existingForB.setCourseOffering(otherOffering);
        existingForB.setStatus(RegistrationStatus.REGISTERED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(chosenOffering));
        when(enrollmentRepository.findByTermInstanceIdAndSemesterNumberAndCohortCourseIdAndStatus(
                1L, 1, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(List.of(enrollmentA, enrollmentB));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of());
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(2L)).thenReturn(List.of(existingForB));
        when(attendanceRepository.existsByStudentIdAndSubjectId(2L, 2L)).thenReturn(true);
        when(courseRegistrationRepository.save(any(CourseRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.bulkAssignElectiveChoice(1L, 1L, 1L);

        assertThat(response.eligibleStudentCount()).isEqualTo(2);
        assertThat(response.assignedCount()).isEqualTo(1);
        assertThat(response.blockedCount()).isEqualTo(1);
        assertThat(existingForB.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        verify(courseRegistrationRepository, never()).saveAndFlush(any());
    }

    @Test
    void bulkAssignElectiveChoice_skipsStudentAlreadyOnChosenOffering() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA");
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Student student = createStudent(1L, program, cohort);
        StudentTermEnrollment enrollment = createEnrollment(1L, student, ti, cohort, 1);

        Subject subject = createSubject(1L, "Community Health Elective", "ELECA");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumElectiveGroup group = new CurriculumElectiveGroup(cv, 1, "Term 1 Electives", "T1E");
        group.setId(1L);
        CurriculumSemesterCourse csc = createElectiveCsc(1L, cv, 1, subject, group);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);
        offering.setCurriculumSemesterCourse(csc);

        CourseRegistration existing = new CourseRegistration();
        existing.setId(50L);
        existing.setStudentTermEnrollment(enrollment);
        existing.setCourseOffering(offering);
        existing.setStatus(RegistrationStatus.REGISTERED);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(ti));
        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.findByTermInstanceIdAndSemesterNumberAndCohortCourseIdAndStatus(
                1L, 1, 1L, EnrollmentStatus.ENROLLED))
            .thenReturn(List.of(enrollment));
        when(courseRegistrationRepository.findByStudentTermEnrollmentId(1L)).thenReturn(List.of(existing));

        var response = service.bulkAssignElectiveChoice(1L, 1L, 1L);

        assertThat(response.eligibleStudentCount()).isEqualTo(1);
        assertThat(response.assignedCount()).isEqualTo(0);
        assertThat(response.blockedCount()).isEqualTo(0);
        verify(courseRegistrationRepository, never()).save(any());
    }
}
