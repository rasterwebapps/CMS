package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.cms.config.JpaConfig;
import com.cms.dto.FacultyAbsenceListItemResponse;
import com.cms.model.AcademicYear;
import com.cms.model.ClassSchedule;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.FacultyAbsence;
import com.cms.model.Period;
import com.cms.model.SessionOccurrence;
import com.cms.model.Speciality;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.OccurrenceStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.AcademicYearRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.DayMappingOverrideRepository;
import com.cms.repository.DesignationRepository;
import com.cms.repository.FacultyAbsenceRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.repository.SpecialityRepository;
import com.cms.repository.SubjectRepository;
import com.cms.repository.TermInstanceRepository;

/** FacultyAbsenceService#findPage -- the Faculty Absence list screen's new query/filter logic
 *  (V563 / FACULTY_ABSENCE_VIEW). Runs against a real (in-memory) DB via @DataJpaTest rather than
 *  mocks, since the thing actually worth testing here is the Specification's generated SQL (date
 *  range, faculty-name search, and the substituteApplied EXISTS/NOT EXISTS subquery against
 *  SessionOccurrence.facultyAbsence) -- a Mockito mock of the repository can't catch a criteria
 *  query that builds correctly but matches the wrong rows. */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class FacultyAbsenceServiceFindPageTest {

    @Autowired private FacultyAbsenceRepository facultyAbsenceRepository;
    @Autowired private FacultyRepository facultyRepository;
    @Autowired private SpecialityRepository specialityRepository;
    @Autowired private DesignationRepository designationRepository;
    @Autowired private AcademicYearRepository academicYearRepository;
    @Autowired private TermInstanceRepository termInstanceRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private PeriodRepository periodRepository;
    @Autowired private ClassScheduleRepository classScheduleRepository;
    @Autowired private SessionOccurrenceRepository sessionOccurrenceRepository;
    @Autowired private FacultyAvailabilityRepository facultyAvailabilityRepository;
    @Autowired private DayMappingOverrideRepository dayMappingOverrideRepository;

    private FacultyAbsenceService service;

    private Faculty doe;
    private Faculty roe;
    private ClassSchedule doeMondaySchedule;

    @BeforeEach
    void setUp() {
        service = new FacultyAbsenceService(facultyAbsenceRepository, classScheduleRepository,
            facultyRepository, facultyAvailabilityRepository, sessionOccurrenceRepository,
            mock(AuditLogService.class), dayMappingOverrideRepository);

        Speciality speciality = specialityRepository.save(new Speciality("Nursing", "NUR_FP", "Nursing Dept", null, null));
        DesignationMaster designation = designationRepository.findByCodeIgnoreCase("ASSISTANT_PROFESSOR")
            .orElseGet(() -> designationRepository.save(new DesignationMaster("Assistant Professor", "ASSISTANT_PROFESSOR", null)));

        Faculty newDoe = new Faculty("EMPFP1", "John", "Doe", "john.fp@college.edu", "1111111111",
            speciality, designation, "Nursing", null, null, FacultyStatus.ACTIVE);
        newDoe.setJoiningDate(LocalDate.of(2020, 1, 1));
        doe = facultyRepository.save(newDoe);

        Faculty newRoe = new Faculty("EMPFP2", "Jane", "Roe", "jane.fp@college.edu", "2222222222",
            speciality, designation, "Nursing", null, null, FacultyStatus.ACTIVE);
        newRoe.setJoiningDate(LocalDate.of(2020, 1, 1));
        roe = facultyRepository.save(newRoe);

        AcademicYear ay = academicYearRepository.save(new AcademicYear("2024-2025-FP", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false));
        TermInstance term = termInstanceRepository.save(
            new TermInstance(ay, TermType.ODD, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN));
        Subject subject = subjectRepository.save(new Subject("Nursing Foundations FP", "NF101FP", 4, 3, 1, speciality, 1));
        Period newPeriod = new Period("1st Period FP", LocalTime.of(9, 0), LocalTime.of(10, 0), 1);
        newPeriod.setDurationMinutes(60);
        Period period = periodRepository.save(newPeriod);

        doeMondaySchedule = new ClassSchedule();
        doeMondaySchedule.setFaculty(doe);
        doeMondaySchedule.setSubject(subject);
        doeMondaySchedule.setSessionType(ClassSessionType.THEORY);
        doeMondaySchedule.setDayOfWeek(DayOfWeek.MONDAY);
        doeMondaySchedule.setTermInstance(term);
        doeMondaySchedule.setPeriod(period);
        doeMondaySchedule.setStatus(ClassScheduleStatus.PUBLISHED);
        doeMondaySchedule.setIsActive(true);
        doeMondaySchedule = classScheduleRepository.save(doeMondaySchedule);
    }

    private FacultyAbsence absence(Faculty faculty, LocalDate date) {
        return facultyAbsenceRepository.save(new FacultyAbsence(faculty, date, "Test reason", "tester"));
    }

    @Test
    void shouldFilterByDateRangeInclusive() {
        absence(doe, LocalDate.of(2024, 8, 1));
        absence(doe, LocalDate.of(2024, 8, 5));
        absence(doe, LocalDate.of(2024, 8, 10));

        Page<FacultyAbsenceListItemResponse> page = service.findPage(
            LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 5), null, null, PageRequest.of(0, 25));

        assertThat(page.getContent()).extracting(FacultyAbsenceListItemResponse::absenceDate)
            .containsExactlyInAnyOrder(LocalDate.of(2024, 8, 1), LocalDate.of(2024, 8, 5));
    }

    @Test
    void shouldSearchByFacultyFirstOrLastNameCaseInsensitively() {
        absence(doe, LocalDate.of(2024, 8, 1));
        absence(roe, LocalDate.of(2024, 8, 1));

        Page<FacultyAbsenceListItemResponse> byFirstName = service.findPage(null, null, "doe", null, PageRequest.of(0, 25));
        assertThat(byFirstName.getContent()).extracting(FacultyAbsenceListItemResponse::facultyName).containsExactly("John Doe");

        Page<FacultyAbsenceListItemResponse> byPartialLastName = service.findPage(null, null, "RO", null, PageRequest.of(0, 25));
        assertThat(byPartialLastName.getContent()).extracting(FacultyAbsenceListItemResponse::facultyName).containsExactly("Jane Roe");
    }

    @Test
    void shouldFilterBySubstituteAppliedViaExistsSubquery() {
        FacultyAbsence covered = absence(doe, LocalDate.of(2024, 8, 1));
        FacultyAbsence uncovered = absence(roe, LocalDate.of(2024, 8, 1));

        SessionOccurrence occurrence = new SessionOccurrence(doeMondaySchedule, LocalDate.of(2024, 8, 1));
        occurrence.setEffectiveFaculty(roe);
        occurrence.setFacultyAbsence(covered);
        occurrence.setOccurrenceStatus(OccurrenceStatus.SUBSTITUTED);
        sessionOccurrenceRepository.save(occurrence);

        Page<FacultyAbsenceListItemResponse> substituted = service.findPage(null, null, null, true, PageRequest.of(0, 25));
        assertThat(substituted.getContent()).extracting(FacultyAbsenceListItemResponse::id).containsExactly(covered.getId());

        Page<FacultyAbsenceListItemResponse> notSubstituted = service.findPage(null, null, null, false, PageRequest.of(0, 25));
        assertThat(notSubstituted.getContent()).extracting(FacultyAbsenceListItemResponse::id).containsExactly(uncovered.getId());
    }

    @Test
    void shouldReportAffectedAndSubstitutedSessionCountsPerRow() {
        // doe's only session is the Monday THEORY schedule -- an absence on a Monday inside term
        // bounds should show 1 affected session, uncovered (no SessionOccurrence row at all yet).
        FacultyAbsence a = absence(doe, LocalDate.of(2024, 8, 5)); // a Monday within term bounds

        Page<FacultyAbsenceListItemResponse> page = service.findPage(null, null, null, null, PageRequest.of(0, 25));

        FacultyAbsenceListItemResponse row = page.getContent().stream()
            .filter(r -> r.id().equals(a.getId())).findFirst().orElseThrow();
        assertThat(row.affectedSessionCount()).isEqualTo(1);
        assertThat(row.substitutedCount()).isEqualTo(0);
    }

    @Test
    void shouldReturnEmptyPageWhenNoAbsenceMatchesTheDateRange() {
        absence(doe, LocalDate.of(2024, 8, 1));

        Page<FacultyAbsenceListItemResponse> page = service.findPage(
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), null, null, PageRequest.of(0, 25));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }
}
