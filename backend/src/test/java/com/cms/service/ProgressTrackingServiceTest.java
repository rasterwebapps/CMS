package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.LogProgressRequest;
import com.cms.dto.OfferingProgressResponse;
import com.cms.dto.SessionOccurrenceDto;
import com.cms.dto.TermProgressSummaryResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClassSchedule;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.SessionOccurrence;
import com.cms.model.Subject;
import com.cms.model.SyllabusUnit;
import com.cms.model.enums.AttendanceType;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.FacultyRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.repository.SyllabusUnitRepository;

@ExtendWith(MockitoExtension.class)
class ProgressTrackingServiceTest {

    @Mock private SessionOccurrenceRepository sessionOccurrenceRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private SyllabusUnitRepository syllabusUnitRepository;
    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private ClassScheduleOccurrenceService occurrenceService;

    private ProgressTrackingService service;

    private CurriculumSemesterCourse csc;
    private CourseOffering offering;
    private ClassSchedule schedule;

    @BeforeEach
    void setUp() {
        service = new ProgressTrackingService(sessionOccurrenceRepository, classScheduleRepository,
            syllabusUnitRepository, courseOfferingRepository, facultyRepository, occurrenceService);

        csc = new CurriculumSemesterCourse();
        csc.setId(50L);

        Subject subject = new Subject("Nursing Foundations", "NF101", 4, 3, 1, null, 1);
        subject.setId(1L);

        offering = new CourseOffering();
        offering.setId(200L);
        offering.setSubject(subject);
        offering.setCurriculumSemesterCourse(csc);

        schedule = new ClassSchedule();
        schedule.setId(300L);
        schedule.setCourseOffering(offering);
    }

    private SyllabusUnit unitOf(Long id, Integer number, CurriculumSemesterCourse parent) {
        SyllabusUnit unit = new SyllabusUnit(parent, number, "Unit " + number, AttendanceType.THEORY, 5, null, number);
        unit.setId(id);
        return unit;
    }

    @Test
    void shouldLogCoverageForAValidOccurrenceDate() {
        LocalDate date = LocalDate.of(2024, 8, 5);
        SyllabusUnit unit = unitOf(1L, 1, csc);
        LogProgressRequest request = new LogProgressRequest(300L, date, List.of(1L), "Covered intro");

        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(schedule));
        when(occurrenceService.occurrenceDatesFor(schedule, date, date)).thenReturn(List.of(date));
        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(300L, date)).thenReturn(Optional.empty());
        when(syllabusUnitRepository.findAllById(List.of(1L))).thenReturn(List.of(unit));
        when(sessionOccurrenceRepository.save(any(SessionOccurrence.class))).thenAnswer(inv -> {
            SessionOccurrence o = inv.getArgument(0);
            o.setId(999L);
            return o;
        });

        SessionOccurrenceDto response = service.logCoverage(request, null);

        assertThat(response.id()).isEqualTo(999L);
        assertThat(response.classScheduleId()).isEqualTo(300L);
        assertThat(response.coveredUnits()).hasSize(1);
        assertThat(response.coveredUnits().get(0).id()).isEqualTo(1L);
    }

    @Test
    void shouldRejectFutureDate() {
        LocalDate future = LocalDate.now().plusDays(1);
        LogProgressRequest request = new LogProgressRequest(300L, future, List.of(), null);
        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> service.logCoverage(request, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("future");
    }

    @Test
    void shouldRejectDateThatIsNotARealOccurrence() {
        LocalDate date = LocalDate.of(2024, 8, 6);
        LogProgressRequest request = new LogProgressRequest(300L, date, List.of(), null);

        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(schedule));
        when(occurrenceService.occurrenceDatesFor(schedule, date, date)).thenReturn(List.of());

        assertThatThrownBy(() -> service.logCoverage(request, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a real occurrence");
    }

    @Test
    void shouldRejectUnitNotBelongingToThisSubject() {
        LocalDate date = LocalDate.of(2024, 8, 5);
        CurriculumSemesterCourse otherCsc = new CurriculumSemesterCourse();
        otherCsc.setId(999L);
        SyllabusUnit foreignUnit = unitOf(2L, 1, otherCsc);
        LogProgressRequest request = new LogProgressRequest(300L, date, List.of(2L), null);

        when(classScheduleRepository.findById(300L)).thenReturn(Optional.of(schedule));
        when(occurrenceService.occurrenceDatesFor(schedule, date, date)).thenReturn(List.of(date));
        when(sessionOccurrenceRepository.findByClassScheduleIdAndOccurrenceDate(300L, date)).thenReturn(Optional.empty());
        when(syllabusUnitRepository.findAllById(List.of(2L))).thenReturn(List.of(foreignUnit));

        assertThatThrownBy(() -> service.logCoverage(request, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong");
    }

    @Test
    void shouldThrowWhenClassScheduleNotFound() {
        LogProgressRequest request = new LogProgressRequest(999L, LocalDate.of(2024, 8, 5), List.of(), null);
        when(classScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.logCoverage(request, null))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldComputeOfferingProgressPercentage() {
        SyllabusUnit unit1 = unitOf(1L, 1, csc);
        SyllabusUnit unit2 = unitOf(2L, 2, csc);
        SessionOccurrence occurrence = new SessionOccurrence(schedule, LocalDate.of(2024, 8, 5));
        occurrence.setCoveredUnits(java.util.Set.of(unit1));

        when(courseOfferingRepository.findById(200L)).thenReturn(Optional.of(offering));
        when(syllabusUnitRepository.findByCurriculumSemesterCourseIdOrderBySortOrderAscUnitNumberAsc(50L))
            .thenReturn(List.of(unit1, unit2));
        when(sessionOccurrenceRepository.findByClassSchedule_CourseOffering_Id(200L)).thenReturn(List.of(occurrence));

        OfferingProgressResponse response = service.getProgressForOffering(200L);

        assertThat(response.totalUnits()).isEqualTo(2);
        assertThat(response.coveredUnitCount()).isEqualTo(1);
        assertThat(response.percentComplete()).isEqualTo(50.0);
        assertThat(response.units()).anySatisfy(u -> {
            if (u.unitId().equals(1L)) assertThat(u.covered()).isTrue();
            if (u.unitId().equals(2L)) assertThat(u.covered()).isFalse();
        });
    }

    @Test
    void shouldSkipElectiveAndZeroUnitOfferingsInTermSummary() {
        CurriculumSemesterCourse electiveCsc = new CurriculumSemesterCourse();
        electiveCsc.setId(60L);
        electiveCsc.setIsElective(true);
        Subject electiveSubject = new Subject("Elective X", "ELX", 4, 3, 1, null, 1);
        electiveSubject.setId(2L);
        CourseOffering electiveOffering = new CourseOffering();
        electiveOffering.setId(201L);
        electiveOffering.setSubject(electiveSubject);
        electiveOffering.setCurriculumSemesterCourse(electiveCsc);

        SyllabusUnit unit1 = unitOf(1L, 1, csc);
        SessionOccurrence occurrence = new SessionOccurrence(schedule, LocalDate.of(2024, 8, 5));
        occurrence.setCoveredUnits(java.util.Set.of(unit1));

        when(courseOfferingRepository.findByTermInstanceIdAndIsActiveTrue(10L))
            .thenReturn(List.of(offering, electiveOffering));
        when(courseOfferingRepository.findById(200L)).thenReturn(Optional.of(offering));
        when(syllabusUnitRepository.findByCurriculumSemesterCourseIdOrderBySortOrderAscUnitNumberAsc(50L))
            .thenReturn(List.of(unit1));
        when(sessionOccurrenceRepository.findByClassSchedule_CourseOffering_Id(200L)).thenReturn(List.of(occurrence));

        TermProgressSummaryResponse response = service.getOverallProgressSummary(10L);

        assertThat(response.subjects()).hasSize(1);
        assertThat(response.subjects().get(0).courseOfferingId()).isEqualTo(200L);
        assertThat(response.overallPercentComplete()).isEqualTo(100.0);
    }
}
