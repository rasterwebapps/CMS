package com.cms.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.SyllabusUnitPlanResponse;
import com.cms.dto.UnitVarianceDto;
import com.cms.exception.ResourceNotFoundException;
import com.cms.model.ClassSchedule;
import com.cms.model.CourseOffering;
import com.cms.model.CurriculumSemesterCourse;
import com.cms.model.SessionOccurrence;
import com.cms.model.SyllabusUnit;
import com.cms.model.SyllabusUnitPlan;
import com.cms.model.TermInstance;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.CourseOfferingRepository;
import com.cms.repository.SessionOccurrenceRepository;
import com.cms.repository.SyllabusUnitPlanRepository;
import com.cms.repository.SyllabusUnitRepository;

/**
 * The portion-completion "blueprint": a frozen Planned completion date per {@link SyllabusUnit}
 * for one {@link CourseOffering} (see {@link SyllabusUnitPlan}), generated once from that
 * offering's real timetable occurrences, plus a live Projected-or-Actual recompute to measure
 * drift against it. Planned never moves once generated (only an explicit regenerate replaces it);
 * Projected is recomputed fresh on every read against whatever the timetable currently allows --
 * if a holiday removes sessions after the blueprint was frozen, every not-yet-complete unit's
 * projected date shifts forward together (a full cascade), which is the point: it's the plain
 * "did this holiday push things back, and by how much" signal.
 */
@Service
@Transactional(readOnly = true)
public class PortionBlueprintService {

    private final SyllabusUnitPlanRepository syllabusUnitPlanRepository;
    private final SyllabusUnitRepository syllabusUnitRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassScheduleOccurrenceService occurrenceService;
    private final SessionOccurrenceRepository sessionOccurrenceRepository;

    public PortionBlueprintService(SyllabusUnitPlanRepository syllabusUnitPlanRepository,
                                    SyllabusUnitRepository syllabusUnitRepository,
                                    CourseOfferingRepository courseOfferingRepository,
                                    ClassScheduleRepository classScheduleRepository,
                                    ClassScheduleOccurrenceService occurrenceService,
                                    SessionOccurrenceRepository sessionOccurrenceRepository) {
        this.syllabusUnitPlanRepository = syllabusUnitPlanRepository;
        this.syllabusUnitRepository = syllabusUnitRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.classScheduleRepository = classScheduleRepository;
        this.occurrenceService = occurrenceService;
        this.sessionOccurrenceRepository = sessionOccurrenceRepository;
    }

    /** Freezes a new blueprint for this offering, replacing any previously generated one outright
     *  (delete-then-reinsert) -- intended to run once, after the term's Skeleton/ClassSchedules
     *  are finalized; not re-run automatically on later edits. */
    @Transactional
    public List<SyllabusUnitPlanResponse> generateBlueprint(Long courseOfferingId) {
        CourseOffering offering = requireOffering(courseOfferingId);
        List<SyllabusUnit> units = unitsFor(offering);
        if (units.isEmpty()) {
            throw new IllegalArgumentException("This subject has no syllabus units defined");
        }

        List<OccurrenceHour> timeline = buildTimeline(offering, offering.getTermInstance().getStartDate(),
            offering.getTermInstance().getEndDate());

        syllabusUnitPlanRepository.deleteByCourseOfferingId(courseOfferingId);
        List<SyllabusUnitPlan> plans = computePlan(units, timeline, offering);
        return syllabusUnitPlanRepository.saveAll(plans).stream().map(this::toResponse).toList();
    }

    public List<SyllabusUnitPlanResponse> getBlueprint(Long courseOfferingId) {
        return syllabusUnitPlanRepository.findByCourseOfferingIdOrderBySequenceIndexAsc(courseOfferingId).stream()
            .map(this::toResponse).toList();
    }

    /** Planned (frozen) vs. Projected-or-Actual (live) per unit. Empty if no blueprint has been
     *  generated for this offering yet. */
    public List<UnitVarianceDto> getProjection(Long courseOfferingId) {
        List<SyllabusUnitPlan> frozen = syllabusUnitPlanRepository
            .findByCourseOfferingIdOrderBySequenceIndexAsc(courseOfferingId);
        if (frozen.isEmpty()) {
            return List.of();
        }
        CourseOffering offering = requireOffering(courseOfferingId);
        List<SyllabusUnit> units = unitsFor(offering);

        List<OccurrenceHour> currentTimeline = buildTimeline(offering, offering.getTermInstance().getStartDate(),
            offering.getTermInstance().getEndDate());
        Map<Long, SyllabusUnitPlan> projectedByUnit = computePlan(units, currentTimeline, offering).stream()
            .collect(Collectors.toMap(p -> p.getSyllabusUnit().getId(), Function.identity()));
        Map<Long, LocalDate> plannedByUnit = frozen.stream()
            .collect(Collectors.toMap(p -> p.getSyllabusUnit().getId(), SyllabusUnitPlan::getPlannedCompletionDate));
        Map<Long, LocalDate> actualCompletionByUnit = actualCompletionDates(courseOfferingId);

        return units.stream().map(unit -> {
            LocalDate planned = plannedByUnit.get(unit.getId());
            boolean completed = actualCompletionByUnit.containsKey(unit.getId());
            LocalDate projectedOrActual = completed
                ? actualCompletionByUnit.get(unit.getId())
                : (projectedByUnit.containsKey(unit.getId())
                    ? projectedByUnit.get(unit.getId()).getPlannedCompletionDate() : null);
            Integer varianceDays = (planned != null && projectedOrActual != null)
                ? (int) ChronoUnit.DAYS.between(planned, projectedOrActual) : null;
            return new UnitVarianceDto(unit.getId(), unit.getUnitNumber(), unit.getTitle(),
                planned, projectedOrActual, completed, varianceDays);
        }).toList();
    }

    /** Sum of hours still needed for not-yet-complete units vs. hours actually still available in
     *  the current timeline from today to term end -- the per-subject building block
     *  {@code PortionShortfallService} rolls up across a cohort-semester. Public so the shortfall
     *  service can reuse it without duplicating the timeline/completion logic. */
    double remainingShortfallHours(Long courseOfferingId) {
        List<SyllabusUnitPlan> frozen = syllabusUnitPlanRepository
            .findByCourseOfferingIdOrderBySequenceIndexAsc(courseOfferingId);
        if (frozen.isEmpty()) {
            return 0.0; // no blueprint generated -- nothing to measure against yet
        }
        CourseOffering offering = requireOffering(courseOfferingId);
        Set<Long> completedUnitIds = actualCompletionDates(courseOfferingId).keySet();

        double remainingPlannedHours = frozen.stream()
            .filter(p -> !completedUnitIds.contains(p.getSyllabusUnit().getId()))
            .mapToInt(p -> unitPlannedHours(p.getSyllabusUnit()))
            .sum();

        LocalDate today = LocalDate.now();
        LocalDate termEnd = offering.getTermInstance().getEndDate();
        double remainingAvailableHours = today.isAfter(termEnd) ? 0.0
            : buildTimeline(offering, today, termEnd).stream().mapToDouble(OccurrenceHour::hours).sum();

        return Math.max(0.0, remainingPlannedHours - remainingAvailableHours);
    }

    private int unitPlannedHours(SyllabusUnit unit) {
        return unit.getPlannedHours() != null ? unit.getPlannedHours() : 0;
    }

    private Map<Long, LocalDate> actualCompletionDates(Long courseOfferingId) {
        Map<Long, LocalDate> result = new HashMap<>();
        for (SessionOccurrence occurrence : sessionOccurrenceRepository.findByClassSchedule_CourseOffering_Id(courseOfferingId)) {
            occurrence.getUnitCoverages().stream()
                .filter(c -> Boolean.TRUE.equals(c.getMarkedComplete()))
                .forEach(c -> result.merge(c.getSyllabusUnit().getId(), occurrence.getOccurrenceDate(),
                    (a, b) -> a.isBefore(b) ? a : b));
        }
        return result;
    }

    private List<SyllabusUnit> unitsFor(CourseOffering offering) {
        CurriculumSemesterCourse csc = offering.getCurriculumSemesterCourse();
        if (csc == null) {
            throw new IllegalArgumentException("This offering has no resolved curriculum mapping");
        }
        return syllabusUnitRepository.findByCurriculumSemesterCourseIdOrderBySortOrderAscUnitNumberAsc(csc.getId());
    }

    /** Every PUBLISHED session's real occurrence dates for this offering, each paired with that
     *  session's period duration in hours, sorted chronologically -- the timeline the cumulative
     *  hours walk below consumes. Reuses {@link ClassScheduleOccurrenceService}, which is already
     *  period/block-aware (skips holiday- and manually-blocked dates precisely). */
    private List<OccurrenceHour> buildTimeline(CourseOffering offering, LocalDate from, LocalDate to) {
        List<ClassSchedule> schedules = classScheduleRepository.findByCourseOfferingId(offering.getId()).stream()
            .filter(cs -> cs.getStatus() == ClassScheduleStatus.PUBLISHED)
            .toList();
        List<OccurrenceHour> timeline = new ArrayList<>();
        for (ClassSchedule schedule : schedules) {
            double hours = schedule.getPeriod().getDurationMinutes() / 60.0;
            for (LocalDate date : occurrenceService.occurrenceDatesFor(schedule, from, to)) {
                timeline.add(new OccurrenceHour(date, hours));
            }
        }
        timeline.sort(Comparator.comparing(OccurrenceHour::date));
        return timeline;
    }

    /** Walks the timeline once, accumulating hours, assigning each unit (in curriculum order) the
     *  first date at which cumulative available hours reach that unit's cumulative plannedHours
     *  threshold. A unit whose threshold is never reached within the timeline (not enough
     *  sessions) is simply omitted -- a real signal the curriculum doesn't fit the term as
     *  scheduled, surfaced via the shortfall check rather than papered over here. */
    private List<SyllabusUnitPlan> computePlan(List<SyllabusUnit> units, List<OccurrenceHour> timeline,
                                                CourseOffering offering) {
        List<SyllabusUnitPlan> plans = new ArrayList<>();
        double cumulativeAvailable = 0;
        double neededSoFar = 0;
        int timelineIdx = 0;
        int sequenceIndex = 0;

        for (SyllabusUnit unit : units) {
            sequenceIndex++;
            neededSoFar += unitPlannedHours(unit);
            LocalDate completionDate = null;
            while (timelineIdx < timeline.size()) {
                OccurrenceHour next = timeline.get(timelineIdx++);
                cumulativeAvailable += next.hours();
                if (cumulativeAvailable >= neededSoFar) {
                    completionDate = next.date();
                    break;
                }
            }
            if (completionDate != null) {
                SyllabusUnitPlan plan = new SyllabusUnitPlan();
                plan.setCourseOffering(offering);
                plan.setSyllabusUnit(unit);
                plan.setPlannedCompletionDate(completionDate);
                plan.setPlannedCumulativeHours((int) Math.round(neededSoFar));
                plan.setSequenceIndex(sequenceIndex);
                plans.add(plan);
            }
        }
        return plans;
    }

    private CourseOffering requireOffering(Long courseOfferingId) {
        return courseOfferingRepository.findById(courseOfferingId)
            .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + courseOfferingId));
    }

    private SyllabusUnitPlanResponse toResponse(SyllabusUnitPlan plan) {
        SyllabusUnit unit = plan.getSyllabusUnit();
        return new SyllabusUnitPlanResponse(unit.getId(), unit.getUnitNumber(), unit.getTitle(),
            plan.getPlannedCompletionDate(), plan.getPlannedCumulativeHours(), plan.getSequenceIndex());
    }

    private record OccurrenceHour(LocalDate date, double hours) {
    }
}
