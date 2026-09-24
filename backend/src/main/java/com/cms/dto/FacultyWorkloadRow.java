package com.cms.dto;

/** One faculty's row in the advisory capacity-planning report for a term — see
 *  {@link com.cms.service.FacultyWorkloadCapacityService} for how each figure is computed. */
public record FacultyWorkloadRow(
    Long facultyId,
    String facultyName,
    String designationName,
    double demandHoursPerWeek,
    double committedHoursPerWeek,
    double blockedHoursPerWeek,
    boolean capacityConfigured,
    Double effectiveCapacityHours,
    Double netCapacityHours,
    boolean overDemand,
    boolean overCommitted,
    /** Real session (period-row) count this faculty currently has this week -- the same unit
     *  {@code effectiveMinSessions} is configured in, so this and the floor are always directly
     *  comparable, unlike committedHoursPerWeek (variable-length periods). */
    int actualSessionsPerWeek,
    boolean minSessionsConfigured,
    Integer effectiveMinSessions,
    /** Advisory only -- never blocks anything, unlike overCommitted's hard-cap counterpart. */
    boolean belowMinimum
) {}
