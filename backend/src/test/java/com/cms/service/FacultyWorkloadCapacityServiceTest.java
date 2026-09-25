package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.ClassScheduleResponse;
import com.cms.dto.FacultyWorkloadReportResponse;
import com.cms.dto.FacultyWorkloadRow;
import com.cms.model.ClassSchedule;
import com.cms.model.CourseOffering;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.FacultyAvailability;
import com.cms.model.Period;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.PeriodRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class FacultyWorkloadCapacityServiceTest {

    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private FacultyAvailabilityRepository facultyAvailabilityRepository;
    @Mock private TimetableGlobalAutoScheduleService timetableGlobalAutoScheduleService;
    @Mock private SystemConfigurationService systemConfigurationService;
    @Mock private TimetableSkeletonService timetableSkeletonService;
    @Mock private PeriodRepository periodRepository;
    @Mock private CourseOfferingRepository courseOfferingRepository;

    private FacultyWorkloadCapacityService service;
    private TermInstance term;

    @BeforeEach
    void setUp() {
        service = new FacultyWorkloadCapacityService(termInstanceRepository,
            classScheduleRepository, facultyRepository, facultyAvailabilityRepository, timetableGlobalAutoScheduleService,
            systemConfigurationService, timetableSkeletonService, periodRepository, courseOfferingRepository);

        term = new TermInstance();
        term.setId(10L);
        // Exactly 4 whole weeks, so weeksInTerm() = 4 with no rounding surprises.
        term.setStartDate(LocalDate.of(2026, 1, 1));
        term.setEndDate(LocalDate.of(2026, 1, 28));

        when(termInstanceRepository.findById(10L)).thenReturn(java.util.Optional.of(term));
        lenient().when(timetableGlobalAutoScheduleService.getTermTotalDemandByFaculty(10L)).thenReturn(Map.of());
        lenient().when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(eq(10L), org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of());
        lenient().when(timetableSkeletonService.findClinicalShiftGridEntries(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of());
        Period uniformPeriod = new Period();
        uniformPeriod.setDurationMinutes(50);
        lenient().when(periodRepository.findByIsActiveTrueOrderByPeriodOrderAsc()).thenReturn(List.of(uniformPeriod));
    }

    private ClassScheduleResponse clinicalShiftEntry(Long facultyId, DayOfWeek dayOfWeek, LocalTime start, LocalTime end) {
        return new ClassScheduleResponse(-1L, ClassSessionType.CLINICAL, ClassScheduleStatus.DRAFT,
            null, null, 5L, "Child Health Nursing I", "N-CHN-I",
            facultyId, "Faculty", null, "Clinical Shift (Monday)", start, end,
            "Clinical - Section 1 - Batch 1", 305L, null, 3L, "SKS Hospital Ward 4",
            73L, 3, dayOfWeek, 10L, null, true, null, null);
    }

    private CourseOffering courseOfferingWithClinicalDuration(Long id, Integer durationMinutes) {
        CourseOffering offering = new CourseOffering();
        offering.setId(id);
        offering.setClinicalShiftDurationMinutes(durationMinutes);
        return offering;
    }

    private Faculty faculty(Long id, String name, DesignationMaster designation, Integer override) {
        Faculty f = new Faculty();
        f.setId(id);
        f.setFirstName(name);
        f.setLastName("");
        f.setDesignation(designation);
        f.setPlannedWeeklySessionsOverride(override);
        return f;
    }

    private DesignationMaster designation(Integer defaultHours) {
        DesignationMaster d = new DesignationMaster("Professor", "PROF", null);
        d.setId(1L);
        d.setDefaultWeeklyTeachingSessions(defaultHours);
        return d;
    }

    private ClassSchedule schedule(Faculty faculty, int durationMinutes) {
        ClassSchedule cs = new ClassSchedule();
        cs.setFaculty(faculty);
        Period period = new Period();
        period.setDurationMinutes(durationMinutes);
        cs.setPeriod(period);
        return cs;
    }

    private FacultyAvailability block(Faculty faculty, LocalTime start, LocalTime end) {
        FacultyAvailability fa = new FacultyAvailability();
        fa.setFaculty(faculty);
        fa.setDayOfWeek(DayOfWeek.MONDAY);
        fa.setStartTime(start);
        fa.setEndTime(end);
        return fa;
    }

    @Test
    void shouldComputeDemandOnlyBeforeAnyStaffingHappens() {
        DesignationMaster designation = designation(20);
        Faculty f = faculty(1L, "Jane", designation, null);

        // 80 term-total curriculum hours over a 4-week term => 20 hours/week demand.
        when(timetableGlobalAutoScheduleService.getTermTotalDemandByFaculty(10L)).thenReturn(Map.of(1L, 80.0));
        when(facultyRepository.findAllById(java.util.Set.of(1L))).thenReturn(List.of(f));
        when(facultyAvailabilityRepository.findByFacultyIdInOrderByDayOfWeekAscStartTimeAsc(List.of(1L)))
            .thenReturn(List.of());

        FacultyWorkloadReportResponse report = service.getTermWorkloadReport(10L);

        assertThat(report.rows()).hasSize(1);
        FacultyWorkloadRow row = report.rows().get(0);
        assertThat(row.demandHoursPerWeek()).isEqualTo(20.0);
        assertThat(row.committedHoursPerWeek()).isEqualTo(0.0);
        assertThat(row.capacityConfigured()).isTrue();
        assertThat(row.netCapacityHours()).isEqualTo(20.0);
        assertThat(row.overDemand()).isFalse();
    }

    @Test
    void shouldComputeCommittedOnlyFromPlacedSchedules() {
        DesignationMaster designation = designation(10);
        Faculty f = faculty(2L, "Sam", designation, null);

        // Two 60-minute weekly sessions => 2.0 hours/week committed.
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(schedule(f, 60), schedule(f, 60)));
        when(facultyRepository.findAllById(java.util.Set.of(2L))).thenReturn(List.of(f));
        when(facultyAvailabilityRepository.findByFacultyIdInOrderByDayOfWeekAscStartTimeAsc(List.of(2L)))
            .thenReturn(List.of());

        FacultyWorkloadReportResponse report = service.getTermWorkloadReport(10L);

        FacultyWorkloadRow row = report.rows().get(0);
        assertThat(row.demandHoursPerWeek()).isEqualTo(0.0);
        assertThat(row.committedHoursPerWeek()).isEqualTo(2.0);
        assertThat(row.overCommitted()).isFalse();
    }

    @Test
    void shouldFlagUnconfiguredFacultyWithoutFalsePositive() {
        Faculty f = faculty(3L, "Alex", null, null);

        when(timetableGlobalAutoScheduleService.getTermTotalDemandByFaculty(10L)).thenReturn(Map.of(3L, 200.0));
        when(facultyRepository.findAllById(java.util.Set.of(3L))).thenReturn(List.of(f));
        when(facultyAvailabilityRepository.findByFacultyIdInOrderByDayOfWeekAscStartTimeAsc(List.of(3L)))
            .thenReturn(List.of());

        FacultyWorkloadReportResponse report = service.getTermWorkloadReport(10L);

        FacultyWorkloadRow row = report.rows().get(0);
        assertThat(row.capacityConfigured()).isFalse();
        assertThat(row.netCapacityHours()).isNull();
        assertThat(row.overDemand()).isFalse();
        assertThat(row.overCommitted()).isFalse();
        assertThat(report.unconfiguredFacultyCount()).isEqualTo(1);
    }

    @Test
    void facultyOverrideShouldWinOverDesignationDefault() {
        DesignationMaster designation = designation(10);
        Faculty f = faculty(4L, "Priya", designation, 25);

        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(schedule(f, 60)));
        when(facultyRepository.findAllById(java.util.Set.of(4L))).thenReturn(List.of(f));
        when(facultyAvailabilityRepository.findByFacultyIdInOrderByDayOfWeekAscStartTimeAsc(List.of(4L)))
            .thenReturn(List.of());

        FacultyWorkloadReportResponse report = service.getTermWorkloadReport(10L);

        assertThat(report.rows().get(0).effectiveCapacityHours()).isEqualTo(25.0);
    }

    @Test
    void shouldNetOutBlockedAvailabilityHoursAndFlagOverCommitted() {
        DesignationMaster designation = designation(10);
        Faculty f = faculty(5L, "Ravi", designation, null);

        // 12 hours/week committed against a 10-hour capacity minus 2 hours blocked (net 8) => over.
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(schedule(f, 60 * 12)));
        when(facultyRepository.findAllById(java.util.Set.of(5L))).thenReturn(List.of(f));
        when(facultyAvailabilityRepository.findByFacultyIdInOrderByDayOfWeekAscStartTimeAsc(List.of(5L)))
            .thenReturn(List.of(block(f, LocalTime.of(9, 0), LocalTime.of(11, 0))));

        FacultyWorkloadReportResponse report = service.getTermWorkloadReport(10L);

        FacultyWorkloadRow row = report.rows().get(0);
        assertThat(row.blockedHoursPerWeek()).isEqualTo(2.0);
        assertThat(row.netCapacityHours()).isEqualTo(8.0);
        assertThat(row.overCommitted()).isTrue();
    }

    @Test
    void shouldFlagBelowMinimumUsingDesignationDefaultFloor() {
        DesignationMaster designation = designation(20);
        designation.setDefaultMinWeeklySessions(16);
        Faculty f = faculty(6L, "Arjun", designation, null);

        // Only 2 real sessions this week, well under the designation's 16-session floor.
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(schedule(f, 60), schedule(f, 60)));
        when(facultyRepository.findAllById(java.util.Set.of(6L))).thenReturn(List.of(f));
        when(facultyAvailabilityRepository.findByFacultyIdInOrderByDayOfWeekAscStartTimeAsc(List.of(6L)))
            .thenReturn(List.of());

        FacultyWorkloadReportResponse report = service.getTermWorkloadReport(10L);

        FacultyWorkloadRow row = report.rows().get(0);
        assertThat(row.actualSessionsPerWeek()).isEqualTo(2);
        assertThat(row.minSessionsConfigured()).isTrue();
        assertThat(row.effectiveMinSessions()).isEqualTo(16);
        assertThat(row.belowMinimum()).isTrue();
    }

    @Test
    void shouldNotFlagBelowMinimumWhenActualMeetsTheFloorExactly() {
        DesignationMaster designation = designation(20);
        designation.setDefaultMinWeeklySessions(2);
        Faculty f = faculty(7L, "Meera", designation, null);

        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(schedule(f, 60), schedule(f, 60)));
        when(facultyRepository.findAllById(java.util.Set.of(7L))).thenReturn(List.of(f));
        when(facultyAvailabilityRepository.findByFacultyIdInOrderByDayOfWeekAscStartTimeAsc(List.of(7L)))
            .thenReturn(List.of());

        FacultyWorkloadReportResponse report = service.getTermWorkloadReport(10L);

        FacultyWorkloadRow row = report.rows().get(0);
        assertThat(row.actualSessionsPerWeek()).isEqualTo(2);
        assertThat(row.belowMinimum()).isFalse();
    }

    @Test
    void shouldFallBackToInstitutionWideMinWhenNoFacultyOrDesignationFloorConfigured() {
        DesignationMaster designation = designation(20); // no defaultMinWeeklySessions set
        Faculty f = faculty(8L, "Divya", designation, null);

        when(systemConfigurationService.findByKey("timetable.faculty_min_weekly_sessions"))
            .thenReturn(java.util.Optional.of(new com.cms.dto.SystemConfigurationResponse(
                1L, "timetable.faculty_min_weekly_sessions", "10", "desc",
                com.cms.model.enums.ConfigDataType.INTEGER, "TIMETABLE", true, null, null)));
        when(classScheduleRepository.findByTermInstanceIdAndStatusAndIsActiveTrue(10L, ClassScheduleStatus.PUBLISHED))
            .thenReturn(List.of(schedule(f, 60)));
        when(facultyRepository.findAllById(java.util.Set.of(8L))).thenReturn(List.of(f));
        when(facultyAvailabilityRepository.findByFacultyIdInOrderByDayOfWeekAscStartTimeAsc(List.of(8L)))
            .thenReturn(List.of());

        FacultyWorkloadReportResponse report = service.getTermWorkloadReport(10L);

        FacultyWorkloadRow row = report.rows().get(0);
        assertThat(row.effectiveMinSessions()).isEqualTo(10);
        assertThat(row.belowMinimum()).isTrue();
    }

    /** OC-263: Clinical Shift Group duty never produces a real ClassSchedule row (see
     *  ClinicalShiftOccurrenceService), so without folding in findClinicalShiftGridEntries, a
     *  faculty coordinating one read as if they had no committed hours/sessions at all no matter
     *  how much duty they actually carried. Periods-equivalent is fractional (370min / 50min-period
     *  = 7.4), per explicit product direction -- not rounded to 7 or 8. */
    @Test
    void shouldFoldClinicalShiftDutyIntoCommittedHoursAndFractionalSessionCount() {
        DesignationMaster designation = designation(20);
        Faculty f = faculty(11L, "Shiva", designation, null);

        // Row's own startTime/endTime (7:00-15:00, a 8h bus-inclusive window) is deliberately
        // wider than the raw 370min/6h10m clinicalShiftDurationMinutes stubbed below -- the fix
        // must use the raw curriculum duration for hours/periods, not the displayed window.
        when(timetableSkeletonService.findClinicalShiftGridEntries(10L, ClassScheduleStatus.DRAFT))
            .thenReturn(List.of(
                clinicalShiftEntry(11L, DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(15, 0)), // this faculty
                clinicalShiftEntry(99L, DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(15, 0)))); // a different faculty -- excluded
        when(courseOfferingRepository.findById(73L)).thenReturn(java.util.Optional.of(
            courseOfferingWithClinicalDuration(73L, 370)));
        when(facultyRepository.findAllById(java.util.Set.of(11L, 99L))).thenReturn(List.of(f));
        lenient().when(facultyAvailabilityRepository.findByFacultyIdInOrderByDayOfWeekAscStartTimeAsc(org.mockito.ArgumentMatchers.anyList()))
            .thenReturn(List.of());

        FacultyWorkloadReportResponse report = service.getTermWorkloadReport(10L);

        FacultyWorkloadRow row = report.rows().get(0);
        assertThat(row.committedHoursPerWeek()).isEqualTo(370.0 / 60.0);
        assertThat(row.actualSessionsPerWeek()).isEqualTo(7.4, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void shouldLeaveBelowMinimumFalseWhenNoFloorConfiguredAnywhere() {
        DesignationMaster designation = designation(20); // no defaultMinWeeklySessions set
        Faculty f = faculty(9L, "Karthik", designation, null);

        // Some demand so this faculty shows up in the report at all (rows are keyed off demand or
        // committed presence, same as every other test in this file).
        when(timetableGlobalAutoScheduleService.getTermTotalDemandByFaculty(10L)).thenReturn(Map.of(9L, 40.0));
        lenient().when(systemConfigurationService.findByKey("timetable.faculty_min_weekly_sessions"))
            .thenReturn(java.util.Optional.empty());
        when(facultyRepository.findAllById(java.util.Set.of(9L))).thenReturn(List.of(f));
        when(facultyAvailabilityRepository.findByFacultyIdInOrderByDayOfWeekAscStartTimeAsc(List.of(9L)))
            .thenReturn(List.of());

        FacultyWorkloadReportResponse report = service.getTermWorkloadReport(10L);

        FacultyWorkloadRow row = report.rows().get(0);
        assertThat(row.minSessionsConfigured()).isFalse();
        assertThat(row.effectiveMinSessions()).isNull();
        assertThat(row.belowMinimum()).isFalse();
    }
}
