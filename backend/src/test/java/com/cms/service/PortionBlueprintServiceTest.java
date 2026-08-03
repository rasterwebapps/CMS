package com.cms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cms.dto.SyllabusUnitPlanResponse;
import com.cms.dto.UnitVarianceDto;
import com.cms.model.AcademicYear;
import com.cms.model.ClassSchedule;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.Period;
import com.cms.model.SessionOccurrence;
import com.cms.model.SessionOccurrenceUnit;
import com.cms.model.Subject;
import com.cms.model.SyllabusUnit;
import com.cms.model.SyllabusUnitPlan;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.repository.SyllabusUnitPlanRepository;
import com.cms.repository.SyllabusUnitRepository;

@ExtendWith(MockitoExtension.class)
class PortionBlueprintServiceTest {

    @Mock private SyllabusUnitPlanRepository syllabusUnitPlanRepository;
    @Mock private SyllabusUnitRepository syllabusUnitRepository;
    @Mock private CourseOfferingRepository courseOfferingRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private ClassScheduleOccurrenceService occurrenceService;
    @Mock private SessionOccurrenceRepository sessionOccurrenceRepository;

    private PortionBlueprintService service;

    private CourseOffering offering;
    private SyllabusUnit unit1;
    private SyllabusUnit unit2;
    private ClassSchedule schedule;

    @BeforeEach
    void setUp() {
        service = new PortionBlueprintService(syllabusUnitPlanRepository, syllabusUnitRepository,
            courseOfferingRepository, classScheduleRepository, occurrenceService, sessionOccurrenceRepository);

        AcademicYear ay = new AcademicYear("2025-2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), true);
        ay.setId(1L);
        TermInstance term = new TermInstance(ay, TermType.ODD, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), TermInstanceStatus.OPEN);
        term.setId(10L);

        CurriculumSemesterCourse csc = new CurriculumSemesterCourse();
        csc.setId(100L);
        csc.setIsElective(false);

        Subject subject = new Subject();
        subject.setId(200L);
        subject.setName("Anatomy");
        subject.setCode("ANAT101");

        offering = new CourseOffering();
        offering.setId(300L);
        offering.setTermInstance(term);
        offering.setCurriculumSemesterCourse(csc);
        offering.setSubject(subject);

        unit1 = new SyllabusUnit(csc, 1, "Unit 1", com.cms.model.enums.AttendanceType.THEORY, 2, null, 1);
        unit1.setId(1000L);
        unit2 = new SyllabusUnit(csc, 2, "Unit 2", com.cms.model.enums.AttendanceType.THEORY, 3, null, 2);
        unit2.setId(1001L);

        Period period = new Period();
        period.setId(500L);
        period.setDurationMinutes(60);

        schedule = new ClassSchedule();
        schedule.setId(400L);
        schedule.setStatus(ClassScheduleStatus.PUBLISHED);
        schedule.setPeriod(period);
        schedule.setCourseOffering(offering);
        schedule.setSessionType(ClassSessionType.THEORY);

        // lenient(): not every test in this class exercises the offering/unit/schedule lookup
        // path (e.g. the "no blueprint exists yet" shortfall test returns before reaching it).
        org.mockito.Mockito.lenient().when(courseOfferingRepository.findById(300L)).thenReturn(Optional.of(offering));
        org.mockito.Mockito.lenient().when(syllabusUnitRepository.findByCurriculumSemesterCourseIdOrderBySortOrderAscUnitNumberAsc(100L))
            .thenReturn(List.of(unit1, unit2));
        org.mockito.Mockito.lenient().when(classScheduleRepository.findByCourseOfferingId(300L)).thenReturn(List.of(schedule));
    }

    @Test
    void shouldAssignPlannedCompletionDatesByWalkingCumulativeHours() {
        when(occurrenceService.occurrenceDatesFor(schedule, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .thenReturn(List.of(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 4), LocalDate.of(2026, 1, 5)));
        when(syllabusUnitPlanRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<SyllabusUnitPlanResponse> result = service.generateBlueprint(300L);

        assertThat(result).hasSize(2);
        // Unit 1 needs 2h -- reached after the 2nd occurrence (Jan 2).
        assertThat(result.get(0).plannedCompletionDate()).isEqualTo(LocalDate.of(2026, 1, 2));
        // Unit 2 needs a further 3h (cumulative 5h) -- reached after the 5th occurrence (Jan 5).
        assertThat(result.get(1).plannedCompletionDate()).isEqualTo(LocalDate.of(2026, 1, 5));

        ArgumentCaptor<List<SyllabusUnitPlan>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(syllabusUnitPlanRepository).deleteByCourseOfferingId(300L);
        org.mockito.Mockito.verify(syllabusUnitPlanRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void shouldOmitAUnitWhoseThresholdIsNeverReachedWithinTheTimeline() {
        // Only 2 hours ever become available -- unit 1 (needs 2h) fits, unit 2 (needs 5h
        // cumulative) never does.
        when(occurrenceService.occurrenceDatesFor(schedule, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .thenReturn(List.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)));
        when(syllabusUnitPlanRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<SyllabusUnitPlanResponse> result = service.generateBlueprint(300L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).unitId()).isEqualTo(unit1.getId());
    }

    @Test
    void projectionShouldCascadeForwardWhenLaterSessionsAreLost() {
        // Frozen blueprint: unit1 -> Jan 2, unit2 -> Jan 5 (5 daily 1h sessions Jan 1-5).
        SyllabusUnitPlan frozenUnit1 = plan(unit1, LocalDate.of(2026, 1, 2), 2, 1);
        SyllabusUnitPlan frozenUnit2 = plan(unit2, LocalDate.of(2026, 1, 5), 5, 2);
        when(syllabusUnitPlanRepository.findByCourseOfferingIdOrderBySequenceIndexAsc(300L))
            .thenReturn(List.of(frozenUnit1, frozenUnit2));
        when(sessionOccurrenceRepository.findByClassSchedule_CourseOffering_Id(300L)).thenReturn(List.of());

        // Current timeline: Jan 4-5 got blocked (e.g. an emergency holiday), replaced by Jan 6-7
        // later in the month -- same 5 total hours, but unit2 now finishes 2 days later.
        when(occurrenceService.occurrenceDatesFor(schedule, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .thenReturn(List.of(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 7)));

        List<UnitVarianceDto> variance = service.getProjection(300L);

        assertThat(variance).hasSize(2);
        UnitVarianceDto v1 = variance.get(0);
        assertThat(v1.plannedCompletionDate()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(v1.projectedOrActualDate()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(v1.varianceDays()).isEqualTo(0);

        UnitVarianceDto v2 = variance.get(1);
        assertThat(v2.plannedCompletionDate()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(v2.projectedOrActualDate()).isEqualTo(LocalDate.of(2026, 1, 7));
        assertThat(v2.varianceDays()).isEqualTo(2);
        assertThat(v2.completed()).isFalse();
    }

    @Test
    void projectionShouldPreferRealLoggedCompletionOverAProjectedDate() {
        SyllabusUnitPlan frozenUnit1 = plan(unit1, LocalDate.of(2026, 1, 2), 2, 1);
        when(syllabusUnitPlanRepository.findByCourseOfferingIdOrderBySequenceIndexAsc(300L))
            .thenReturn(List.of(frozenUnit1));
        when(syllabusUnitRepository.findByCurriculumSemesterCourseIdOrderBySortOrderAscUnitNumberAsc(100L))
            .thenReturn(List.of(unit1));
        when(occurrenceService.occurrenceDatesFor(schedule, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .thenReturn(List.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)));

        SessionOccurrence occurrence = new SessionOccurrence(schedule, LocalDate.of(2026, 1, 1));
        SessionOccurrenceUnit coverage = new SessionOccurrenceUnit(occurrence, unit1, new BigDecimal("1.0"), true);
        occurrence.getUnitCoverages().add(coverage);
        when(sessionOccurrenceRepository.findByClassSchedule_CourseOffering_Id(300L)).thenReturn(List.of(occurrence));

        List<UnitVarianceDto> variance = service.getProjection(300L);

        assertThat(variance).hasSize(1);
        assertThat(variance.get(0).completed()).isTrue();
        // Actually marked complete on Jan 1, a day earlier than either the planned or projected date.
        assertThat(variance.get(0).projectedOrActualDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void remainingShortfallHoursShouldBeZeroWhenNoBlueprintExists() {
        when(syllabusUnitPlanRepository.findByCourseOfferingIdOrderBySequenceIndexAsc(300L)).thenReturn(List.of());

        assertThat(service.remainingShortfallHours(300L)).isEqualTo(0.0);
    }

    private SyllabusUnitPlan plan(SyllabusUnit unit, LocalDate date, int cumulativeHours, int sequenceIndex) {
        SyllabusUnitPlan plan = new SyllabusUnitPlan();
        plan.setCourseOffering(offering);
        plan.setSyllabusUnit(unit);
        plan.setPlannedCompletionDate(date);
        plan.setPlannedCumulativeHours(cumulativeHours);
        plan.setSequenceIndex(sequenceIndex);
        return plan;
    }
}
