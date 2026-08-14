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
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.CourseOfferingDto;
import com.cms.dto.GenerateOfferingsResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.Course;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.CurriculumVersion;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Program;
import com.cms.model.Speciality;
import com.cms.model.StudentTermEnrollment;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.EnrollmentStatus;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.CurriculumVersionRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class CourseOfferingServiceImplTest {

    @Mock
    private CourseOfferingRepository courseOfferingRepository;
    @Mock
    private TermInstanceRepository termInstanceRepository;
    @Mock
    private CohortRepository cohortRepository;
    @Mock
    private CurriculumVersionRepository curriculumVersionRepository;
    @Mock
    private CurriculumSemesterCourseRepository curriculumSemesterCourseRepository;
    @Mock
    private FacultyRepository facultyRepository;
    @Mock
    private StudentTermEnrollmentRepository studentTermEnrollmentRepository;

    private CourseOfferingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourseOfferingServiceImpl(
            courseOfferingRepository, termInstanceRepository, cohortRepository,
            curriculumVersionRepository, curriculumSemesterCourseRepository, facultyRepository,
            studentTermEnrollmentRepository);
    }

    private Speciality createSpeciality(Long id, String name, String code) {
        Speciality s = new Speciality(name, code, name + " Dept", null, null);
        s.setId(id);
        return s;
    }

    private Faculty createFaculty(Long id, Speciality speciality) {
        DesignationMaster designation = new DesignationMaster("Assistant Professor", "ASSISTANT_PROFESSOR", null);
        designation.setId(1L);
        Faculty f = new Faculty("EMP0" + id, "Fac", "Ulty" + id, "fac" + id + "@college.edu", "1234567890",
            speciality, designation, null, null, null, FacultyStatus.ACTIVE);
        f.setId(id);
        return f;
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

    private Course createCourse(Long id, String name, String code, Program program) {
        Course course = new Course(name, code, null, program);
        course.setId(id);
        return course;
    }

    private Cohort createCohort(Long id, Course course, AcademicYear admissionAY) {
        Cohort c = new Cohort();
        c.setId(id);
        c.setCourse(course);
        c.setAdmissionAcademicYear(admissionAY);
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

    private Subject createSubject(Long id, String name, String code) {
        Subject s = new Subject();
        s.setId(id);
        s.setName(name);
        s.setCode(code);
        return s;
    }

    private CurriculumVersion createCV(Long id, Program program, Course course, AcademicYear ay) {
        return createCV(id, program, course, ay, "CV-2024");
    }

    private CurriculumVersion createCV(Long id, Program program, Course course, AcademicYear ay, String versionName) {
        CurriculumVersion cv = new CurriculumVersion(program, course, versionName, ay, true);
        cv.setId(id);
        cv.setCreatedAt(Instant.now());
        return cv;
    }

    private CurriculumSemesterCourse createCSC(Long id, CurriculumVersion cv, Subject subject, int semNum) {
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse(cv, semNum, subject, 1);
        csc.setId(id);
        return csc;
    }

    private CourseOffering createOffering(Long id, TermInstance ti, CurriculumVersion cv, Subject subject, int semNum) {
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
    void generateOfferingsForTermInstance_createsOfferingsForOddTerm() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumSemesterCourse csc = createCSC(1L, cv, subject, 1);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(termInstance));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(curriculumVersionRepository.findByProgramIdAndCourseIdAndIsActiveTrue(1L, 1L)).thenReturn(List.of(cv));
        when(curriculumSemesterCourseRepository.findByCurriculumVersionId(1L)).thenReturn(List.of(csc));
        when(courseOfferingRepository
            .findByTermInstanceIdAndCurriculumVersionIdAndSubjectIdAndSemesterNumber(1L, 1L, 1L, 1))
            .thenReturn(Optional.empty());
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenAnswer(inv -> {
            CourseOffering o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        GenerateOfferingsResponse result = service.generateOfferingsForTermInstance(1L);

        assertThat(result.offeringsCreated()).isEqualTo(1);
        verify(courseOfferingRepository).save(any(CourseOffering.class));
    }

    @Test
    void generateOfferingsForTermInstance_skipsEvenSemestersForOddTerm() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Soft Skills", "SS102");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        // Even semester number — should be skipped for ODD term
        CurriculumSemesterCourse csc = createCSC(1L, cv, subject, 2);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(termInstance));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(curriculumVersionRepository.findByProgramIdAndCourseIdAndIsActiveTrue(1L, 1L)).thenReturn(List.of(cv));
        when(curriculumSemesterCourseRepository.findByCurriculumVersionId(1L)).thenReturn(List.of(csc));

        GenerateOfferingsResponse result = service.generateOfferingsForTermInstance(1L);

        assertThat(result.offeringsCreated()).isEqualTo(0);
        verify(courseOfferingRepository, never()).save(any());
    }

    @Test
    void generateOfferingsForTermInstance_isIdempotent() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CurriculumSemesterCourse csc = createCSC(1L, cv, subject, 1);
        CourseOffering existing = createOffering(1L, termInstance, cv, subject, 1);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(termInstance));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(curriculumVersionRepository.findByProgramIdAndCourseIdAndIsActiveTrue(1L, 1L)).thenReturn(List.of(cv));
        when(curriculumSemesterCourseRepository.findByCurriculumVersionId(1L)).thenReturn(List.of(csc));
        when(courseOfferingRepository
            .findByTermInstanceIdAndCurriculumVersionIdAndSubjectIdAndSemesterNumber(1L, 1L, 1L, 1))
            .thenReturn(Optional.of(existing));

        GenerateOfferingsResponse result = service.generateOfferingsForTermInstance(1L);

        assertThat(result.offeringsCreated()).isEqualTo(0);
        assertThat(result.offeringsAlreadyExisting()).isEqualTo(1);
        verify(courseOfferingRepository, never()).save(any());
    }

    @Test
    void generateOfferingsForTermInstance_skipsCohortWithNoActiveCurriculumVersion() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(termInstance));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(curriculumVersionRepository.findByProgramIdAndCourseIdAndIsActiveTrue(1L, 1L)).thenReturn(List.of());

        GenerateOfferingsResponse result = service.generateOfferingsForTermInstance(1L);

        assertThat(result.offeringsCreated()).isEqualTo(0);
        assertThat(result.cohortsWithoutCurriculumVersion()).containsExactly(cohort.getDisplayName());
        verify(courseOfferingRepository, never()).save(any());
    }

    @Test
    void generateOfferingsForTermInstance_scopesToCohortsExactCourseUnderSharedProgram() {
        // Regression test for MSc Nursing (Adult) / (Child): both share one Program but each has
        // its own course-scoped CurriculumVersion, so a course-specific lookup must resolve only
        // the Adult cohort's own version, never the Child course's.
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "MSc", 2);
        Course adultCourse = createCourse(1L, "MSc Nursing (Adult)", "MSN-A", program);
        Cohort cohort = createCohort(1L, adultCourse, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);

        Subject adultSubject = createSubject(1L, "Advanced Medical Surgical Nursing", "MSNA101");
        CurriculumVersion courseSpecificCV = createCV(10L, program, adultCourse, ay, "MSc Adult V1");
        CurriculumSemesterCourse adultCsc = createCSC(1L, courseSpecificCV, adultSubject, 1);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(termInstance));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(curriculumVersionRepository.findByProgramIdAndCourseIdAndIsActiveTrue(1L, 1L))
            .thenReturn(List.of(courseSpecificCV));
        when(curriculumSemesterCourseRepository.findByCurriculumVersionId(10L)).thenReturn(List.of(adultCsc));
        when(courseOfferingRepository
            .findByTermInstanceIdAndCurriculumVersionIdAndSubjectIdAndSemesterNumber(1L, 10L, 1L, 1))
            .thenReturn(Optional.empty());
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenAnswer(inv -> {
            CourseOffering o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        GenerateOfferingsResponse result = service.generateOfferingsForTermInstance(1L);

        assertThat(result.offeringsCreated()).isEqualTo(1);
    }

    @Test
    void generateOfferingsForTermInstance_throwsWhenTermInstanceNotFound() {
        when(termInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateOfferingsForTermInstance(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void getOfferingsByTermInstance_returnsMappedDtos() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);

        when(courseOfferingRepository.findByTermInstanceId(1L)).thenReturn(List.of(offering));

        List<CourseOfferingDto> dtos = service.getOfferingsByTermInstance(1L);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).termNumber()).isEqualTo(1);
        assertThat(dtos.get(0).subjectCode()).isEqualTo("MATH101");
    }

    @Test
    void getOfferingsByTermInstanceAndSemester_returnsMappedDtos() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);

        when(courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(1L, 1)).thenReturn(List.of(offering));

        List<CourseOfferingDto> dtos = service.getOfferingsByTermInstanceAndSemester(1L, 1);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).termNumber()).isEqualTo(1);
    }

    /** Regression test for the reported bug: a shared TermInstance can concurrently host another
     *  cohort/program's offerings at the exact same semesterNumber (e.g. BSc Nursing's Term 1 and
     *  a different regulation's Term 3 mapped subjects both landing on "2026-2027 Odd"). Unlike
     *  {@link #getOfferingsByTermInstanceAndSemester_returnsMappedDtos}, this cohort-scoped lookup
     *  must resolve only the requested cohort's own curriculum version and its actual enrolled
     *  semester — never the other cohort's offering, even though both share termInstanceId and
     *  semesterNumber. */
    @Test
    void getOfferingsByTermInstanceAndCohort_excludesAnotherCohortsOfferingAtTheSameSemesterNumber() {
        AcademicYear ay = createAY(1L, "2026-2027");
        Program bscProgram = createProgram(1L, "BSc Nursing", 4);
        Course bscCourse = createCourse(1L, "BSc Nursing INC 2020", "BSCN-INC20", bscProgram);
        Cohort bscCohort = createCohort(1L, bscCourse, ay);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);

        Subject englishSubject = createSubject(1L, "Communicative English", "ENGL101");
        CurriculumVersion bscCv = createCV(10L, bscProgram, bscCourse, ay, "INC 2020 V1");
        CourseOffering bscOffering = createOffering(100L, ti, bscCv, englishSubject, 1);

        // A different program/regulation's own semester-1 offering, sharing the same TermInstance
        // and the same semesterNumber, but a completely different CurriculumVersion.
        Program otherProgram = createProgram(2L, "GNM", 3);
        Course otherCourse = createCourse(2L, "GNM 2021 Reg", "GNM-2021", otherProgram);
        Subject unrelatedSubject = createSubject(2L, "Adult Health Nursing I", "N-AHN-I-215");
        CurriculumVersion otherCv = createCV(20L, otherProgram, otherCourse, ay, "GNM V1");
        CourseOffering otherOffering = createOffering(200L, ti, otherCv, unrelatedSubject, 1);

        StudentTermEnrollment enrollment = new StudentTermEnrollment();
        enrollment.setTermInstance(ti);
        enrollment.setCohort(bscCohort);
        enrollment.setSemesterNumber(1);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);

        when(cohortRepository.findById(1L)).thenReturn(Optional.of(bscCohort));
        when(curriculumVersionRepository.findByProgramIdAndCourseIdAndIsActiveTrue(1L, 1L)).thenReturn(List.of(bscCv));
        when(studentTermEnrollmentRepository.findByTermInstanceIdAndCohortId(1L, 1L)).thenReturn(List.of(enrollment));
        when(courseOfferingRepository.findByTermInstanceIdAndCurriculumVersionIdAndSemesterNumberIn(1L, 10L, Set.of(1)))
            .thenReturn(List.of(bscOffering)); // repository itself is expected to exclude otherOffering (different CV)

        List<CourseOfferingDto> dtos = service.getOfferingsByTermInstanceAndCohort(1L, 1L);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).subjectCode()).isEqualTo("ENGL101");
        assertThat(dtos).extracting(CourseOfferingDto::subjectCode).doesNotContain(otherOffering.getSubject().getCode());
    }

    @Test
    void getOfferingsByTermInstanceAndCohort_returnsEmptyWhenCohortHasNoActiveCurriculumVersion() {
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        Cohort cohort = createCohort(1L, course, createAY(1L, "2026-2027"));

        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));
        when(curriculumVersionRepository.findByProgramIdAndCourseIdAndIsActiveTrue(1L, 1L)).thenReturn(List.of());

        List<CourseOfferingDto> dtos = service.getOfferingsByTermInstanceAndCohort(1L, 1L);

        assertThat(dtos).isEmpty();
        verify(studentTermEnrollmentRepository, never()).findByTermInstanceIdAndCohortId(any(), any());
    }

    @Test
    void getById_returnsDto() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);

        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));

        CourseOfferingDto dto = service.getById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.termInstanceLabel()).isEqualTo("2024-2025 ODD");
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(courseOfferingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void updateOffering_updatesFacultyAndSection() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);

        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenReturn(offering);

        service.updateOffering(1L, 42L, null, "Section A");

        assertThat(offering.getFacultyId()).isEqualTo(42L);
        assertThat(offering.getSectionLabel()).isEqualTo("Section A");
    }

    @Test
    void updateOffering_blocksFacultyFromADifferentSpecialityThanTheSubject() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Speciality nursingSpeciality = createSpeciality(1L, "Nursing", "NUR");
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursingSpeciality, 1);
        subject.setId(1L);
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);
        Speciality csSpeciality = createSpeciality(2L, "Computer Science", "CS");
        Faculty mismatchedFaculty = createFaculty(42L, csSpeciality);

        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(facultyRepository.findById(42L)).thenReturn(Optional.of(mismatchedFaculty));

        assertThatThrownBy(() -> service.updateOffering(1L, 42L, null, "Section A"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not eligible to teach");

        verify(courseOfferingRepository, never()).save(any());
    }

    @Test
    void updateOffering_allowsFacultyFromTheSameSpecialityAsTheSubject() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Speciality nursingSpeciality = createSpeciality(1L, "Nursing", "NUR");
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursingSpeciality, 1);
        subject.setId(1L);
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);
        Faculty matchingFaculty = createFaculty(42L, nursingSpeciality);

        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(facultyRepository.findById(42L)).thenReturn(Optional.of(matchingFaculty));
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenReturn(offering);

        service.updateOffering(1L, 42L, null, "Section A");

        assertThat(offering.getFacultyId()).isEqualTo(42L);
    }

    @Test
    void updateOffering_grandfathersAnUnchangedMismatchedFaculty() {
        // A row assigned before this eligibility rule existed may already carry a mismatched
        // pairing. Resubmitting the SAME faculty on an otherwise-unrelated edit (section label)
        // must not suddenly start failing, and must not require a facultyRepository lookup.
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Speciality nursingSpeciality = createSpeciality(1L, "Nursing", "NUR");
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursingSpeciality, 1);
        subject.setId(1L);
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);
        offering.setFacultyId(42L);

        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenReturn(offering);

        service.updateOffering(1L, 42L, null, "Section A - Renamed");

        assertThat(offering.getSectionLabel()).isEqualTo("Section A - Renamed");
        verify(facultyRepository, never()).findById(any());
    }

    @Test
    void updateOffering_blocksASecondaryFacultyFromADifferentSpecialityThanTheSubject() {
        // OC-127 gap-closure follow-up: secondaryFacultyId now has the same department-eligibility
        // gate as the primary, checked independently against its own prior value.
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Speciality nursingSpeciality = createSpeciality(1L, "Nursing", "NUR");
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursingSpeciality, 1);
        subject.setId(1L);
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);
        Speciality csSpeciality = createSpeciality(2L, "Computer Science", "CS");
        Faculty mismatchedFaculty = createFaculty(43L, csSpeciality);

        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(facultyRepository.findById(43L)).thenReturn(Optional.of(mismatchedFaculty));

        assertThatThrownBy(() -> service.updateOffering(1L, null, 43L, "Section A"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not eligible to teach");

        verify(courseOfferingRepository, never()).save(any());
    }

    @Test
    void updateOffering_allowsASecondaryFacultyFromTheSameSpecialityAsTheSubject() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Speciality nursingSpeciality = createSpeciality(1L, "Nursing", "NUR");
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursingSpeciality, 1);
        subject.setId(1L);
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);
        Faculty matchingFaculty = createFaculty(43L, nursingSpeciality);

        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(facultyRepository.findById(43L)).thenReturn(Optional.of(matchingFaculty));
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenReturn(offering);

        service.updateOffering(1L, null, 43L, "Section A");

        assertThat(offering.getSecondaryFacultyId()).isEqualTo(43L);
    }

    @Test
    void updateOffering_grandfathersAnUnchangedMismatchedSecondaryFacultyIndependentlyOfThePrimary() {
        // The secondary's own prior value is the grandfather baseline -- not the primary's --
        // so an unrelated edit doesn't suddenly require the secondary to also match.
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Speciality nursingSpeciality = createSpeciality(1L, "Nursing", "NUR");
        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, nursingSpeciality, 1);
        subject.setId(1L);
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);
        offering.setSecondaryFacultyId(43L);

        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenReturn(offering);

        service.updateOffering(1L, null, 43L, "Section A - Renamed");

        assertThat(offering.getSectionLabel()).isEqualTo("Section A - Renamed");
        verify(facultyRepository, never()).findById(any());
    }

    @Test
    void deactivateOffering_setsIsActiveFalse() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);

        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenReturn(offering);

        service.deactivateOffering(1L);

        assertThat(offering.getIsActive()).isFalse();
    }

    @Test
    void deactivateAllOfferingsForTermInstance_deactivatesAll() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, course, ay);
        CourseOffering o1 = createOffering(1L, ti, cv, subject, 1);
        CourseOffering o2 = createOffering(2L, ti, cv, subject, 3);

        when(courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(1L)).thenReturn(List.of(o1, o2));
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deactivateAllOfferingsForTermInstance(1L);

        assertThat(o1.getIsActive()).isFalse();
        assertThat(o2.getIsActive()).isFalse();
    }

    @Test
    void findActiveCohortsWithoutCurriculumVersion_returnsCohortsWithNoActiveVersionMapped() {
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        AcademicYear ay = createAY(1L, "2024-2025");
        Cohort cohort = createCohort(1L, course, ay);

        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(curriculumVersionRepository.findByProgramIdAndCourseIdAndIsActiveTrue(1L, 1L)).thenReturn(List.of());

        List<Cohort> result = service.findActiveCohortsWithoutCurriculumVersion();

        assertThat(result).containsExactly(cohort);
    }

    @Test
    void findActiveCohortsWithoutCurriculumVersion_omitsCohortsThatHaveOne() {
        Program program = createProgram(1L, "BCA", 3);
        Course course = createCourse(1L, "BCA Course", "BCA-C", program);
        AcademicYear ay = createAY(1L, "2024-2025");
        Cohort cohort = createCohort(1L, course, ay);
        CurriculumVersion cv = createCV(1L, program, course, ay);

        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(curriculumVersionRepository.findByProgramIdAndCourseIdAndIsActiveTrue(1L, 1L)).thenReturn(List.of(cv));

        List<Cohort> result = service.findActiveCohortsWithoutCurriculumVersion();

        assertThat(result).isEmpty();
    }
}
