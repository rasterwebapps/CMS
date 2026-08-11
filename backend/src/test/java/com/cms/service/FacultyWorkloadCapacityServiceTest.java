package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.FacultyWorkloadReportResponse;
import com.cms.dto.FacultyWorkloadRow;
import com.cms.model.ClassSchedule;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.DesignationMaster;
import com.cms.model.Faculty;
import com.cms.model.FacultyAvailability;
import com.cms.model.Period;
import com.cms.model.TermInstance;
import com.cms.model.enums.DayOfWeek;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.FacultyAvailabilityRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.TermInstanceRepository;

@ExtendWith(MockitoExtension.class)
class FacultyWorkloadCapacityServiceTest {

    @Mock private TermInstanceRepository termInstanceRepository;
    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private FacultyAvailabilityRepository facultyAvailabilityRepository;

    private FacultyWorkloadCapacityService service;
    private TermInstance term;

    @BeforeEach
    void setUp() {
        service = new FacultyWorkloadCapacityService(termInstanceRepository, courseOfferingRepository,
            classScheduleRepository, facultyRepository, facultyAvailabilityRepository);

        term = new TermInstance();
        term.setId(10L);
        // Exactly 4 whole weeks, so weeksInTerm() = 4 with no rounding surprises.
        term.setStartDate(LocalDate.of(2026, 1, 1));
        term.setEndDate(LocalDate.of(2026, 1, 28));

        when(termInstanceRepository.findById(10L)).thenReturn(java.util.Optional.of(term));
    }

    private Faculty faculty(Long id, String name, DesignationMaster designation, Integer override) {
        Faculty f = new Faculty();
        f.setId(id);
        f.setFirstName(name);
        f.setLastName("");
        f.setDesignation(designation);
        f.setPlannedWeeklyHoursOverride(override);
        return f;
    }

    private DesignationMaster designation(Integer defaultHours) {
        DesignationMaster d = new DesignationMaster("Professor", "PROF", null);
        d.setId(1L);
        d.setDefaultWeeklyTeachingHours(defaultHours);
        return d;
    }

    private CourseOffering offering(Long facultyId, int theoryHours, int labHours, int clinicalHours) {
        CourseOffering offering = new CourseOffering();
        offering.setFacultyId(facultyId);
        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setTheoryHours(theoryHours);
        csc.setLabHours(labHours);
        csc.setClinicalHours(clinicalHours);
        offering.setCurriculumSemesterCourse(csc);
        return offering;
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

        // 80 curriculum hours over a 4-week term => 20 hours/week demand.
        when(courseOfferingRepository.findByTermInstanceId(10L))
            .thenReturn(List.of(offering(1L, 60, 20, 0)));
        when(classScheduleRepository.findByTermInstanceId(10L)).thenReturn(List.of());
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

        when(courseOfferingRepository.findByTermInstanceId(10L)).thenReturn(List.of());
        // Two 60-minute weekly sessions => 2.0 hours/week committed.
        when(classScheduleRepository.findByTermInstanceId(10L))
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

        when(courseOfferingRepository.findByTermInstanceId(10L))
            .thenReturn(List.of(offering(3L, 200, 0, 0)));
        when(classScheduleRepository.findByTermInstanceId(10L)).thenReturn(List.of());
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

        when(courseOfferingRepository.findByTermInstanceId(10L)).thenReturn(List.of());
        when(classScheduleRepository.findByTermInstanceId(10L)).thenReturn(List.of(schedule(f, 60)));
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

        when(courseOfferingRepository.findByTermInstanceId(10L)).thenReturn(List.of());
        // 12 hours/week committed against a 10-hour capacity minus 2 hours blocked (net 8) => over.
        when(classScheduleRepository.findByTermInstanceId(10L))
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
}
