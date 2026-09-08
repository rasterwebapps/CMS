package com.cms.dto;

import java.util.List;

/** One subject where this run had to fall back off its own bound faculty ({@code
 *  originalFacultyId}) onto another already-eligible faculty member ({@code substituteFacultyId})
 *  to actually place {@code sessionCount} session(s) this week — see {@code
 *  TimetableGlobalAutoScheduleService#recordFacultySubstitutionIfAny}. Purely informational: the
 *  substitute sessions are already placed and staffed by the time this is reported, nothing here is
 *  a pending action. Surfaced as a "consider reassigning this offering" tip because a faculty member
 *  who is the bottleneck often enough for the fallback to keep firing is a real signal worth an
 *  admin's attention, not just this run's own workaround.
 *
 * <p>{@code substituteRemainingHours}/{@code substituteCapacityTier} are the substitute's own term
 *  workload BEFORE this run added anything to them (null/{@code "NONE"} when no cap is configured
 *  for them at all, same convention as everywhere else in this codebase — never treat null as "no
 *  capacity left"). {@code substituteTotalSessionsThisRun} is that same substitute's grand total
 *  across EVERY subject they picked up as a fallback this run, which can be larger than this one
 *  tip's own {@code sessionCount} if the same person covered more than one subject — the number to
 *  actually check before trusting them as a permanent reassignment.
 *
 * <p>{@code affectedSections} is the exact, disjoint set of (cohort, section) rows this tip's own
 *  fallback covered — carried unchanged back to {@link ConfirmFacultySubstitutionItem} on Submit so
 *  {@code CourseOfferingSectionFacultyService#confirmSubstitutions} reassigns only these rows, not
 *  every row in the offering still on {@code originalFacultyId} (a sibling section can independently
 *  fall back to a *different* substitute in the same run and get its own separate tip). */
public record FacultySubstitutionTip(
    String subjectName,
    Long originalFacultyId,
    String originalFacultyName,
    Long substituteFacultyId,
    String substituteFacultyName,
    int sessionCount,
    Long courseOfferingId,
    Double substituteRemainingHours,
    String substituteCapacityTier,
    int substituteTotalSessionsThisRun,
    List<SubstitutionAffectedSection> affectedSections
) {}
