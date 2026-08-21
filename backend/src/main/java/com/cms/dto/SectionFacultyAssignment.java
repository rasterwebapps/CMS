package com.cms.dto;

/** {@code cohortName} exists because a CourseOffering can be shared by more than one cohort on
 *  the same curriculum version -- each CohortSection unambiguously belongs to exactly one cohort
 *  regardless, so sections from every matching cohort are listed together, distinguished by this
 *  label rather than picking just one cohort to show. */
public record SectionFacultyAssignment(
    Long cohortSectionId,
    String cohortName,
    String sectionLabel,
    Long facultyId,
    String facultyName
) {}
