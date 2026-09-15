package com.cms.dto;

/**
 * Curriculum Clinical hours a shift-configured offering still owes after BOTH its duty roster and
 * the weekly grid have delivered everything they structurally can — the "no unassigned hours" gap,
 * reported with the one remedy that actually closes it.
 *
 * <p>Why this needs its own item rather than riding on the ordinary unplaced list: the residual is
 * arithmetic, not a scheduling failure, and the ordinary "couldn't place" wording sends an admin
 * hunting for a free slot that would not help. A duty group delivers exactly one occurrence per
 * week, so three duty days over a 26-week term yield 78 occurrences; a 480h subject at a 6h shift
 * needs 80. The 12h shortfall is two duty DAYS, not twelve hours of empty classroom, and it cannot
 * be closed on the weekly grid at all: the smallest weekly clinical unit for such a subject is
 * 3.33h &times; 26 weeks &asymp; 86.7h, which overshoots the residual by roughly 75h. Asking the
 * grid to absorb it is what made these subjects look permanently, inexplicably short.
 *
 * <p>The mechanism that does close it already exists and needs no new model: {@code
 * ClinicalShiftGroup} carries {@code effectiveStartDate}/{@code effectiveEndDate}, so a duty group
 * bounded to a {@code extraDutyDays}-week window generates exactly that many extra occurrences and
 * no more — both the hours-crediting side ({@code TimetableSkeletonService#toClinicalShiftHours})
 * and the real occurrence generator ({@code ClinicalShiftOccurrenceService}) already respect that
 * range and cap against the same hours math, so the calendar and the reported hours stay in step.
 *
 * <p>Deliberately a proposal, not an action the run takes. Creating duty days puts real students on
 * a real bus to a real hospital on a specific date; which dates those are is a decision with a
 * calendar, a ward roster and an escort rotation behind it. The run's job is to make the gap
 * explicit, exact, and closable in one step — not to pick the dates itself.
 */
public record ClinicalResidualItem(
    Long courseOfferingId,
    String subjectName,
    String cohortName,
    /** Curriculum Clinical hours still unaccounted for, after duty roster + grid. Always &gt; 0. */
    double residualHours,
    /** One duty occurrence's own length, from {@code CourseOffering.clinicalShiftDurationMinutes}. */
    double hoursPerDutyDay,
    /** Extra duty occurrences needed to cover {@code residualHours} — what the admin actually adds. */
    int extraDutyDays,
    String remedy,
    /** The offering's current duty length, in minutes. */
    Integer currentDurationMinutes,
    /** The other remedy (OC-227): the shortest duty length, rounded up to 5 minutes, at which the
     *  EXISTING roster alone delivers every curriculum Clinical hour — no extra duty days needed.
     *  Null when lengthening the duty can't help. */
    Integer suggestedDurationMinutes,
    /** True when that longer duty still has students back before any period that's free today — it
     *  costs zero timetable periods (e.g. 07:00 + 6h10m + 60 min bus = 14:10, when Period 6 starts). */
    boolean suggestedDurationCostsNoPeriods
) {}
