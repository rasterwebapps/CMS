package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ConflictScanResponse;
import com.cms.model.AcademicYear;
import com.cms.model.Classroom;
import com.cms.model.ClassSchedule;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.CourseOffering;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.Period;
import com.cms.model.Speciality;
import com.cms.model.Subject;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.model.enums.FacultyStatus;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.BatchRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.ClassroomRepository;
import com.cms.repository.CohortRoomAllocationRepository;
import com.cms.repository.CohortSectionRepository;
import com.cms.repository.CourseRegistrationRepository;
import com.cms.repository.FacultyAbsenceRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.StudentTermEnrollmentRepository;
import com.cms.repository.TermInstanceRepository;

/** Wires a real {@link TimetableStaffingService} (mocked repositories, same as
 *  {@link TimetableStaffingServiceTest}) so the checks it reuses are the exact production
 *  ones, not re-implemented stand-ins. */
@ExtendWith(MockitoExtension.class)
class TimetableConflictInspectorServiceTest {

    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private ClassroomRepository classroomRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private CourseRegistrationRepository courseRegistrationRepository;
    @Mock private StudentTermEnrollmentRepository studentTermEnrollmentRepository;
    @Mock private CohortRoomAllocationRepository cohortRoomAllocationRepository;
    @Mock private CohortSectionRepository cohortSectionRepository;
    @Mock private RotationResolverService rotationResolverService;
    @Mock private TimetableBlockedPeriodChecker blockedPeriodChecker;
    @Mock private FacultyAvailabilityRepository facultyAvailabilityRepository;
    @Mock private FacultyAbsenceRepository facultyAbsenceRepository;
    @Mock private SystemConfigurationService systemConfigurationService;

    private TimetableConflictInspectorService service;

    private TermInstance termInstance;
    private Faculty faculty;
    private Subject subject;
    private Period period;
    private Classroom classroom;

    @BeforeEach
    void setUp() {
        TimetableStaffingService staffingService = new TimetableStaffingService(classScheduleRepository,
            facultyRepository, classroomRepository, batchRepository, courseRegistrationRepository,
            studentTermEnrollmentRepository, cohortRoomAllocationRepository, cohortSectionRepository,
            rotationResolverService, blockedPeriodChecker, facultyAvailabilityRepository,
            facultyAbsenceRepository, systemConfigurationService);
        service = new TimetableConflictInspectorService(classScheduleRepository, termInstanceRepository,
            staffingService, blockedPeriodChecker);

        AcademicYear ay = new AcademicYear("2024-2025", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 5, 31), false);
        ay.setId(1L);
        termInstance = new TermInstance(ay, TermType.ODD, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 11, 30), TermInstanceStatus.OPEN);
        termInstance.setId(10L);

        Speciality speciality = new Speciality("Nursing", "NUR", "Nursing Dept", null, null);
        speciality.setId(1L);
        DesignationMaster designation = new DesignationMaster("Assistant Professor", "ASSISTANT_PROFESSOR", null);
        designation.setId(1L);
        faculty = new Faculty("EMP001", "John", "Doe", "john@college.edu", "1234567890",
            speciality, designation, "Nursing", null, null, FacultyStatus.ACTIVE);
        faculty.setId(1L);

        subject = new Subject("Anatomy", "ANAT101", 4, 3, 1, speciality, 1);
        subject.setId(1L);

        period = new Period("1st Period", LocalTime.of(9, 0), LocalTime.of(9, 50), 1);
        period.setId(1L);

        classroom = new Classroom("Room 101", "Main Block", "101", 60);
        classroom.setId(1L);

        when(termInstanceRepository.findById(10L)).thenReturn(Optional.of(termInstance));
        lenient().when(blockedPeriodChecker.blockReason(any(), any(), any(), any())).thenReturn(Optional.empty());
        lenient().when(classScheduleRepository.findOverlapping(any(), any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
    }

    private ClassSchedule staffedCell(long id) {
        ClassSchedule cs = new ClassSchedule();
        cs.setId(id);
        cs.setSessionType(ClassSessionType.THEORY);
        cs.setStatus(ClassScheduleStatus.PUBLISHED);
        cs.setSubject(subject);
        cs.setDayOfWeek(DayOfWeek.MONDAY);
        cs.setTermInstance(termInstance);
        cs.setPeriod(period);
        cs.setFaculty(faculty);
        cs.setClassroom(classroom);
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setIsElective(true);
        CourseOffering offering = new CourseOffering();
        offering.setTermInstance(termInstance);
        offering.setCurriculumSemesterCourse(csc);
        cs.setCourseOffering(offering);
        return cs;
    }

    @Test
    void shouldReportNoViolationsForAConflictFreeTerm() {
        ClassSchedule cell = staffedCell(100L);
        when(classScheduleRepository.findByTermInstanceId(10L)).thenReturn(List.of(cell));

        ConflictScanResponse result = service.scanTerm(10L);

        assertThat(result.scannedCellCount()).isEqualTo(1);
        assertThat(result.violationCellCount()).isZero();
        assertThat(result.violationCount()).isZero();
        assertThat(result.rows()).isEmpty();
    }

    @Test
    void shouldFlagACellSittingInABlockedPeriod() {
        ClassSchedule cell = staffedCell(100L);
        when(classScheduleRepository.findByTermInstanceId(10L)).thenReturn(List.of(cell));
        when(blockedPeriodChecker.blockReason(DayOfWeek.MONDAY, period.getStartTime(), period.getEndTime(), termInstance))
            .thenReturn(Optional.of("Staff meeting"));

        ConflictScanResponse result = service.scanTerm(10L);

        assertThat(result.violationCellCount()).isEqualTo(1);
        assertThat(result.rows().get(0).violations())
            .extracting(com.cms.dto.ConstraintViolation::code)
            .contains("CONFLICT_PERIOD_BLOCKED");
    }

    @Test
    void shouldFlagTwoCellsThatDoubleBookTheSameFacultyAtTheSameTime() {
        // The exact gap this dashboard closes: two independently-staffed cells, each valid on its
        // own at staffing time, that nonetheless clash with each other once both exist.
        ClassSchedule cellA = staffedCell(100L);
        ClassSchedule cellB = staffedCell(200L);

        when(classScheduleRepository.findByTermInstanceId(10L)).thenReturn(List.of(cellA, cellB));
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, period.getStartTime(), period.getEndTime(),
            ClassScheduleStatus.PUBLISHED, 100L)).thenReturn(List.of(cellB));
        when(classScheduleRepository.findOverlapping(DayOfWeek.MONDAY, 10L, period.getStartTime(), period.getEndTime(),
            ClassScheduleStatus.PUBLISHED, 200L)).thenReturn(List.of(cellA));

        ConflictScanResponse result = service.scanTerm(10L);

        assertThat(result.violationCellCount()).isEqualTo(2);
        assertThat(result.rows()).extracting(row -> row.classScheduleId())
            .containsExactlyInAnyOrder(100L, 200L);
        assertThat(result.rows()).allSatisfy(row ->
            assertThat(row.violations()).extracting(com.cms.dto.ConstraintViolation::code)
                .contains("STAFFING_FACULTY_CONFLICT"));
    }

    @Test
    void shouldSkipFacultyAndRoomChecksForAnUnstaffedDraftCell() {
        ClassSchedule unstaffed = staffedCell(100L);
        unstaffed.setFaculty(null);
        unstaffed.setClassroom(null);
        unstaffed.setStatus(ClassScheduleStatus.DRAFT);
        when(classScheduleRepository.findByTermInstanceId(10L)).thenReturn(List.of(unstaffed));

        ConflictScanResponse result = service.scanTerm(10L);

        assertThat(result.violationCellCount()).isZero();
    }
}
