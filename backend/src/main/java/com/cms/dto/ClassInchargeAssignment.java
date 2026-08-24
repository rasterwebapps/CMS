package com.cms.dto;

/** One committed {@code CohortSection} for a term, with its Class Incharge if assigned.
 *  {@code cohortName} + {@code sectionLabel} together identify the section, since a term can have
 *  many cohorts each with their own sections. */
public record ClassInchargeAssignment(
    Long cohortSectionId,
    String cohortName,
    String sectionLabel,
    String classroomName,
    Long facultyId,
    String facultyName
) {}
