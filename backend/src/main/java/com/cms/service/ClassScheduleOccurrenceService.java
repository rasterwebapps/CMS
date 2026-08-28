package com.cms.service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.model.BlockedPeriod;
import com.cms.model.ClassSchedule;
import com.cms.model.DayMappingOverride;
import com.cms.model.TermInstance;
import com.cms.model.enums.BlockType;
import com.cms.model.enums.ClassScheduleStatus;
import com.cms.repository.BlockedPeriodRepository;
import com.cms.repository.ClassScheduleRepository;
import com.cms.repository.DayMappingOverrideRepository;

/**
 * Resolves the real calendar dates a recurring {@link ClassSchedule} row actually fires on within
 * a window — the piece {@link PersonalTimetableService}'s class javadoc notes doesn't exist yet.
 * Pure computation over {@link ClassSchedule#getDayOfWeek()} + {@link TermInstance} bounds, minus
 * any date on which {@link ClassSchedule#getPeriod()} is covered by a {@link BlockedPeriod} row --
 * holiday-derived (see {@code BlockedPeriod.sourceCalendarEvent}) or manually created alike, since
 * either way no class actually happens then. This is deliberately period-precise rather than the
 * old whole-day HOLIDAY-event check it replaces: a half-day holiday only cancels the periods it
 * actually blocked, and deleting a holiday-derived block for one period on one date (the "unblock
 * for a special class" override) is what makes that session occur again -- no separate flag needed.
 *
 * <p>A second, independent axis also decides whether an already-placed session fires on a given
 * date: a {@link DayMappingOverride} declares that one specific date runs a DIFFERENT weekday's
 * timetable than its own actual weekday (e.g. a compensatory working Saturday running Monday's
 * schedule). This always fully suppresses that date from its own actual-weekday schedules'
 * occurrence lists, and adds it to the borrowed weekday's schedules' occurrence lists instead --
 * see {@link #weeklyDatesInRange} for the exact two-directional resolution. {@link BlockedPeriod}
 * filtering is applied afterward, unchanged, so a borrowed-in date can still be independently
 * blocked.
 *
 * <p>A third axis: a schedule whose {@code dayOfWeek} is SATURDAY only fires on dates matching its
 * {@link TermInstance}'s opt-in {@code workingSaturdayWeeks} pattern (e.g. "1st Saturday of every
 * month") — see {@link WorkingSaturdayCalculator}. An empty pattern means every Saturday is
 * suppressed (the term hasn't opted in); this never applies to a borrowed-IN date from a {@link
 * DayMappingOverride}, since that override is itself a deliberate, explicit decision that this
 * specific Saturday is a working day.
 *
 * <p>Never writes anything, never leaks into generation (which stays a weekly-recurring placement
 * problem per {@link TimetableGenerationService}'s own javadoc).
 */
@Service
@Transactional(readOnly = true)
public class ClassScheduleOccurrenceService {

    private final BlockedPeriodRepository blockedPeriodRepository;
    private final DayMappingOverrideRepository dayMappingOverrideRepository;
    private final ClassScheduleRepository classScheduleRepository;

    public ClassScheduleOccurrenceService(BlockedPeriodRepository blockedPeriodRepository,
                                           DayMappingOverrideRepository dayMappingOverrideRepository,
                                           ClassScheduleRepository classScheduleRepository) {
        this.blockedPeriodRepository = blockedPeriodRepository;
        this.dayMappingOverrideRepository = dayMappingOverrideRepository;
        this.classScheduleRepository = classScheduleRepository;
    }

    /** Distinct PUBLISHED sessions a given faculty member can mark attendance for on a specific
     *  date -- resolved through both {@link DayMappingOverride} borrowing and {@link BlockedPeriod}
     *  suppression, i.e. exactly the same "does this session actually fire on this date" logic
     *  {@link #occurrenceDatesFor} uses, just entered from the date side (one date, all of one
     *  faculty's schedules) instead of the schedule side (one schedule, all its dates). Used by
     *  {@link AttendanceService#findAvailableSubjects} so the attendance-marking screen doesn't
     *  re-derive this resolution independently. */
    public List<ClassSchedule> schedulesEffectiveOn(LocalDate date, Long facultyId) {
        Optional<DayMappingOverride> mapping = dayMappingOverrideRepository.findByMappedDate(date);
        boolean isBorrowedInDate = mapping.isPresent();
        Optional<com.cms.model.enums.DayOfWeek> effectiveDay = mapping
            .map(DayMappingOverride::getBorrowedDayOfWeek)
            .or(() -> date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY
                ? Optional.empty()
                : Optional.of(com.cms.model.enums.DayOfWeek.valueOf(date.getDayOfWeek().name())));
        if (effectiveDay.isEmpty()) {
            return List.of();
        }

        List<ClassSchedule> candidates = classScheduleRepository.findByFacultyIdAndStatusAndDayOfWeek(
            facultyId, ClassScheduleStatus.PUBLISHED, effectiveDay.get());

        List<ClassSchedule> result = new ArrayList<>();
        for (ClassSchedule cs : candidates) {
            TermInstance term = cs.getTermInstance();
            if (date.isBefore(term.getStartDate()) || date.isAfter(term.getEndDate())) {
                continue;
            }
            // A borrowed-in date (see weeklyDatesInRange) is a deliberate compensatory override
            // and is never subject to the Saturday-pattern check; a date that's naturally its own
            // actual Saturday still is.
            if (!isBorrowedInDate && WorkingSaturdayCalculator.isNonWorkingSaturday(date, term)) {
                continue;
            }
            List<BlockedPeriod> blocks = blockedPeriodRepository.findApplicableForPeriodInRange(
                cs.getPeriod().getId(), date, date);
            if (matchingBlock(date, blocks).isEmpty()) {
                result.add(cs);
            }
        }
        return result;
    }

    /** One weekly-recurring date that's blocked rather than skipped silently — the complement of
     *  {@link #occurrenceDatesFor}'s own result set for the same schedule/window. */
    public record CancelledOccurrence(LocalDate date, String reason) {}

    /** Occurrence dates for one schedule, clamped to both the term's own bounds and the
     *  caller-supplied [from, to] window, skipping any date on which this schedule's period is
     *  blocked, and resolved through any {@link DayMappingOverride} covering the window. */
    public List<LocalDate> occurrenceDatesFor(ClassSchedule schedule, LocalDate from, LocalDate to) {
        TermInstance term = schedule.getTermInstance();
        LocalDate start = maxDate(from, term.getStartDate());
        LocalDate end = minDate(to, term.getEndDate());
        if (start.isAfter(end)) {
            return List.of();
        }
        List<BlockedPeriod> blocks = blockedPeriodRepository.findApplicableForPeriodInRange(
            schedule.getPeriod().getId(), start, end);
        Map<LocalDate, DayMappingOverride> mappings = mappingsInRange(from, to);
        return datesFor(schedule, start, end, blocks, mappings);
    }

    /** Batched variant for calendar/grid rendering — fetches each distinct period's blocks once
     *  across the whole batch instead of once per schedule, then reuses that cache for every
     *  schedule sharing the same period (a superset fetch over [from, to] is fine regardless of an
     *  individual schedule's own term bounds). Day-mapping overrides aren't period-scoped, so they
     *  are fetched once for the whole batch rather than per-period. */
    public Map<Long, List<LocalDate>> occurrenceDatesForSchedules(
            List<ClassSchedule> schedules, LocalDate from, LocalDate to) {
        Map<Long, List<BlockedPeriod>> blocksByPeriod = new HashMap<>();
        Map<LocalDate, DayMappingOverride> mappings = mappingsInRange(from, to);
        Map<Long, List<LocalDate>> result = new HashMap<>();

        for (ClassSchedule schedule : schedules) {
            TermInstance term = schedule.getTermInstance();
            LocalDate start = maxDate(from, term.getStartDate());
            LocalDate end = minDate(to, term.getEndDate());
            if (start.isAfter(end)) {
                result.put(schedule.getId(), List.of());
                continue;
            }
            Long periodId = schedule.getPeriod().getId();
            List<BlockedPeriod> blocks = blocksByPeriod.computeIfAbsent(periodId,
                id -> blockedPeriodRepository.findApplicableForPeriodInRange(id, from, to));
            result.put(schedule.getId(), datesFor(schedule, start, end, blocks, mappings));
        }
        return result;
    }

    /** Batched variant of {@link #cancelledDatesFor}, mirroring {@link
     *  #occurrenceDatesForSchedules}'s own batching shape (one block fetch per distinct period,
     *  reused across every schedule sharing it; one day-mapping fetch for the whole batch). Purely
     *  additive — {@link #occurrenceDatesFor}/{@link #occurrenceDatesForSchedules} are untouched by
     *  this, since {@link com.cms.service.PortionBlueprintService}, {@link
     *  com.cms.service.ProgressTrackingService}, and {@link com.cms.service.FacultySessionSwapService}
     *  all depend on their existing excludes-blocked-dates contract. */
    public Map<Long, List<CancelledOccurrence>> cancelledDatesForSchedules(
            List<ClassSchedule> schedules, LocalDate from, LocalDate to) {
        Map<Long, List<BlockedPeriod>> blocksByPeriod = new HashMap<>();
        Map<LocalDate, DayMappingOverride> mappings = mappingsInRange(from, to);
        Map<Long, List<CancelledOccurrence>> result = new HashMap<>();

        for (ClassSchedule schedule : schedules) {
            TermInstance term = schedule.getTermInstance();
            LocalDate start = maxDate(from, term.getStartDate());
            LocalDate end = minDate(to, term.getEndDate());
            if (start.isAfter(end)) {
                result.put(schedule.getId(), List.of());
                continue;
            }
            Long periodId = schedule.getPeriod().getId();
            List<BlockedPeriod> blocks = blocksByPeriod.computeIfAbsent(periodId,
                id -> blockedPeriodRepository.findApplicableForPeriodInRange(id, from, to));
            result.put(schedule.getId(), cancelledDatesFor(schedule, start, end, blocks, mappings));
        }
        return result;
    }

    private Map<LocalDate, DayMappingOverride> mappingsInRange(LocalDate from, LocalDate to) {
        return dayMappingOverrideRepository.findByMappedDateBetween(from, to).stream()
            .collect(java.util.stream.Collectors.toMap(DayMappingOverride::getMappedDate, m -> m));
    }

    private static List<LocalDate> datesFor(ClassSchedule schedule, LocalDate start, LocalDate end,
                                             List<BlockedPeriod> blocks, Map<LocalDate, DayMappingOverride> mappings) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date : weeklyDatesInRange(schedule, start, end, mappings)) {
            if (matchingBlock(date, blocks).isEmpty()) {
                dates.add(date);
            }
        }
        return dates;
    }

    private static List<CancelledOccurrence> cancelledDatesFor(ClassSchedule schedule, LocalDate start, LocalDate end,
                                                                 List<BlockedPeriod> blocks,
                                                                 Map<LocalDate, DayMappingOverride> mappings) {
        List<CancelledOccurrence> cancelled = new ArrayList<>();
        for (LocalDate date : weeklyDatesInRange(schedule, start, end, mappings)) {
            matchingBlock(date, blocks).ifPresent(block -> cancelled.add(new CancelledOccurrence(date, block.getReason())));
        }
        return cancelled;
    }

    /** The raw weekly-recurring date sequence for a schedule within [start, end], with no
     *  blocked-date filtering applied — shared by both {@link #datesFor} (which then excludes
     *  blocked dates) and {@link #cancelledDatesFor} (which keeps only them).
     *
     *  <p>Resolved through {@code mappingsInWindow} in both directions for a schedule with
     *  {@code dayOfWeek = D}: (1) SUPPRESS -- any of D's natural weekly dates that carries a
     *  mapping (to any weekday) is dropped, since that date's own actual-weekday sessions are
     *  fully suppressed once mapped away; (2) BORROW IN -- any date in the window whose mapping's
     *  borrowed weekday equals D is added, since that date is now running D's schedule despite not
     *  naturally falling on D. Both directions read the same single override map, so no
     *  cross-schedule coordination is needed. Direction-2 dates aren't in natural weekly order, so
     *  the combined list is re-sorted. */
    private static List<LocalDate> weeklyDatesInRange(ClassSchedule schedule, LocalDate start, LocalDate end,
                                                        Map<LocalDate, DayMappingOverride> mappingsInWindow) {
        // com.cms.model.enums.DayOfWeek only has MONDAY..SATURDAY (no SUNDAY) -- names line up
        // exactly with java.time.DayOfWeek's, so valueOf() is a safe direct mapping.
        java.time.DayOfWeek targetDay = java.time.DayOfWeek.valueOf(schedule.getDayOfWeek().name());
        LocalDate first = start.with(TemporalAdjusters.nextOrSame(targetDay));
        TermInstance term = schedule.getTermInstance();

        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = first; !date.isAfter(end); date = date.plusWeeks(1)) {
            if (!mappingsInWindow.containsKey(date) && !WorkingSaturdayCalculator.isNonWorkingSaturday(date, term)) {
                dates.add(date);
            }
            // else: this date is either mapped away to a different weekday's schedule, or a
            // Saturday not matching the term's working-Saturday pattern -- either way, no real
            // class happens on it, so it's suppressed entirely from this schedule's own
            // occurrence list (a borrowed-IN date below is a deliberate compensatory override and
            // is never subject to the Saturday-pattern check -- that override IS the authority).
        }

        if (!mappingsInWindow.isEmpty()) {
            Long termInstanceId = schedule.getTermInstance().getId();
            for (Map.Entry<LocalDate, DayMappingOverride> entry : mappingsInWindow.entrySet()) {
                LocalDate date = entry.getKey();
                DayMappingOverride mapping = entry.getValue();
                if (mapping.getBorrowedDayOfWeek() == schedule.getDayOfWeek()
                        && !date.isBefore(start) && !date.isAfter(end)
                        && date.getDayOfWeek() != targetDay
                        && termInstanceId.equals(mapping.getTermInstance().getId())) {
                    dates.add(date);
                }
            }
            dates.sort(Comparator.naturalOrder());
        }
        return dates;
    }

    /** Mirrors the exact ONE_OFF/RECURRING matching logic already used by
     *  {@code TimetableCapacityPlanningService.blockedHoursInTerm} -- kept consistent so both
     *  agree on which dates are actually non-teaching. */
    private static Optional<BlockedPeriod> matchingBlock(LocalDate date, List<BlockedPeriod> blocks) {
        com.cms.model.enums.DayOfWeek appDayOfWeek = com.cms.model.enums.DayOfWeek.valueOf(date.getDayOfWeek().name());
        for (BlockedPeriod block : blocks) {
            boolean matches = block.getBlockType() == BlockType.ONE_OFF
                ? date.equals(block.getSpecificDate())
                : appDayOfWeek == block.getDayOfWeek()
                    && !date.isBefore(block.getRangeStartDate()) && !date.isAfter(block.getRangeEndDate());
            if (matches) {
                return Optional.of(block);
            }
        }
        return Optional.empty();
    }

    private static LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate minDate(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
