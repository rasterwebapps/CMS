package com.cms.dto;

/** One (offering, cohort) pair contributing term hours to a {@link FacultyOverCapacity} faculty's
 *  total demand — a single {@code CourseOffering} can legitimately appear more than once across
 *  different cohorts sharing the same curriculum version, each a distinct audience needing its
 *  own full quota from the one bound faculty. */
public record OverageContributor(
    Long courseOfferingId,
    String subjectName,
    Long cohortId,
    String cohortName,
    double termHoursContributed
) {}
