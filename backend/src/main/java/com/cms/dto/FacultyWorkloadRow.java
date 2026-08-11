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
    boolean overCommitted
) {}
