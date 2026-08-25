package com.cms.dto;

/** One cohort's faculty assignment for an offering -- {@code cohortId} is always set ({@code
 *  cohortName} exists because a CourseOffering can be shared by more than one cohort on the same
 *  curriculum version, each assigned independently); {@code cohortSectionId}/{@code sectionLabel}
 *  are null when the cohort's Theory delivery has no active section split (a single whole-cohort
 *  row), or set to identify exactly which section this row covers when it does. */
public record SectionFacultyAssignment(
    Long cohortId,
    Long cohortSectionId,
    String cohortName,
    String sectionLabel,
    Long facultyId,
    String facultyName
) {}
