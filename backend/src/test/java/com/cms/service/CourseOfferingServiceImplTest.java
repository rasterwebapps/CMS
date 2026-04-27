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

import com.cms.dto.CourseOfferingDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.AcademicYear;
import com.cms.model.Cohort;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.CurriculumVersion;
import com.cms.model.Program;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.CohortStatus;
import com.cms.model.enums.ProgramStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.CohortRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.CurriculumSemesterCourseRepository;
import com.cms.repository.CurriculumVersionRepository;
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

    private CourseOfferingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourseOfferingServiceImpl(
            courseOfferingRepository, termInstanceRepository, cohortRepository,
            curriculumVersionRepository, curriculumSemesterCourseRepository);
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
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
        CurriculumSemesterCourse csc = createCSC(1L, cv, subject, 1);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(termInstance));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(curriculumVersionRepository.findByProgramIdAndIsActiveTrue(1L)).thenReturn(List.of(cv));
        when(curriculumSemesterCourseRepository.findByCurriculumVersionId(1L)).thenReturn(List.of(csc));
        when(courseOfferingRepository
            .findByTermInstanceIdAndCurriculumVersionIdAndSubjectIdAndSemesterNumber(1L, 1L, 1L, 1))
            .thenReturn(Optional.empty());
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenAnswer(inv -> {
            CourseOffering o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        int count = service.generateOfferingsForTermInstance(1L);

        assertThat(count).isEqualTo(1);
        verify(courseOfferingRepository).save(any(CourseOffering.class));
    }

    @Test
    void generateOfferingsForTermInstance_skipsEvenSemestersForOddTerm() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Soft Skills", "SS102");
        CurriculumVersion cv = createCV(1L, program, ay);
        // Even semester number — should be skipped for ODD term
        CurriculumSemesterCourse csc = createCSC(1L, cv, subject, 2);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(termInstance));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(curriculumVersionRepository.findByProgramIdAndIsActiveTrue(1L)).thenReturn(List.of(cv));
        when(curriculumSemesterCourseRepository.findByCurriculumVersionId(1L)).thenReturn(List.of(csc));

        int count = service.generateOfferingsForTermInstance(1L);

        assertThat(count).isEqualTo(0);
        verify(courseOfferingRepository, never()).save(any());
    }

    @Test
    void generateOfferingsForTermInstance_isIdempotent() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
        CurriculumSemesterCourse csc = createCSC(1L, cv, subject, 1);
        CourseOffering existing = createOffering(1L, termInstance, cv, subject, 1);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(termInstance));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(curriculumVersionRepository.findByProgramIdAndIsActiveTrue(1L)).thenReturn(List.of(cv));
        when(curriculumSemesterCourseRepository.findByCurriculumVersionId(1L)).thenReturn(List.of(csc));
        when(courseOfferingRepository
            .findByTermInstanceIdAndCurriculumVersionIdAndSubjectIdAndSemesterNumber(1L, 1L, 1L, 1))
            .thenReturn(Optional.of(existing));

        int count = service.generateOfferingsForTermInstance(1L);

        assertThat(count).isEqualTo(0);
        verify(courseOfferingRepository, never()).save(any());
    }

    @Test
    void generateOfferingsForTermInstance_skipsCohortWithNoActiveCurriculumVersion() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        Cohort cohort = createCohort(1L, program, ay);
        TermInstance termInstance = createTermInstance(1L, ay, TermType.ODD);

        when(termInstanceRepository.findById(1L)).thenReturn(Optional.of(termInstance));
        when(cohortRepository.findByStatus(CohortStatus.ACTIVE)).thenReturn(List.of(cohort));
        when(curriculumVersionRepository.findByProgramIdAndIsActiveTrue(1L)).thenReturn(List.of());

        int count = service.generateOfferingsForTermInstance(1L);

        assertThat(count).isEqualTo(0);
        verify(courseOfferingRepository, never()).save(any());
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
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);

        when(courseOfferingRepository.findByTermInstanceId(1L)).thenReturn(List.of(offering));

        List<CourseOfferingDto> dtos = service.getOfferingsByTermInstance(1L);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).semesterNumber()).isEqualTo(1);
        assertThat(dtos.get(0).subjectCode()).isEqualTo("MATH101");
    }

    @Test
    void getOfferingsByTermInstanceAndSemester_returnsMappedDtos() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);

        when(courseOfferingRepository.findByTermInstanceIdAndSemesterNumber(1L, 1)).thenReturn(List.of(offering));

        List<CourseOfferingDto> dtos = service.getOfferingsByTermInstanceAndSemester(1L, 1);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).semesterNumber()).isEqualTo(1);
    }

    @Test
    void getById_returnsDto() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
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
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
        CourseOffering offering = createOffering(1L, ti, cv, subject, 1);

        when(courseOfferingRepository.findById(1L)).thenReturn(Optional.of(offering));
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenReturn(offering);

        service.updateOffering(1L, 42L, "Section A");

        assertThat(offering.getFacultyId()).isEqualTo(42L);
        assertThat(offering.getSectionLabel()).isEqualTo("Section A");
    }

    @Test
    void deactivateOffering_setsIsActiveFalse() {
        AcademicYear ay = createAY(1L, "2024-2025");
        Program program = createProgram(1L, "BCA", 3);
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
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
        TermInstance ti = createTermInstance(1L, ay, TermType.ODD);
        Subject subject = createSubject(1L, "Math", "MATH101");
        CurriculumVersion cv = createCV(1L, program, ay);
        CourseOffering o1 = createOffering(1L, ti, cv, subject, 1);
        CourseOffering o2 = createOffering(2L, ti, cv, subject, 3);

        when(courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(1L)).thenReturn(List.of(o1, o2));
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deactivateAllOfferingsForTermInstance(1L);

        assertThat(o1.getIsActive()).isFalse();
        assertThat(o2.getIsActive()).isFalse();
    }
}
