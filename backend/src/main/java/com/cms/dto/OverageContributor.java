package com.cms.dto;

/** One (offering, cohort) pair contributing term hours to a {@link FacultyOverCapacity} faculty's
 *  total demand — a single {@code CourseOffering} can legitimately appear more than once across
 *  different cohorts sharing the same curriculum version, each a distinct audience needing its
 *  own full quota from the one bound faculty. {@code cohortSectionId}/{@code batchId} identify
 *  exactly which section (Theory) or batch (Lab/Clinical) this contribution came from — at most
 *  one is non-null, mirroring the THEORY-vs-LAB/CLINICAL split in {@code
 *  TimetableGlobalAutoScheduleService#termHoursForOfferingInCohort} — so a "spread load" action can
 *  reassign that exact section/batch's faculty rather than only knowing the offering+cohort. Both
 *  null means the contribution came from the offering's whole-cohort primary (no active
 *  sections/batches to attribute it to). {@code cohortSectionLabel}/{@code batchName} carry the
 *  matching display name so two rows for the same subject+cohort (e.g. two sections, or several
 *  uncoordinated batches) render distinguishably instead of looking like unexplained duplicates.
 *  {@code sessionType} is "THEORY"/"LAB"/"CLINICAL", or "LAB_CLINICAL" for a legacy untyped batch
 *  or an unsectioned/unbatched offering where lab+clinical hours are combined and can't be split
 *  further; null only for the synthetic single-offering contributor built by {@code
 *  TimetableGlobalAutoScheduleService#checkFacultyCapacityForOffering}, which represents the whole
 *  offering's total rather than one type. */
public record OverageContributor(
    Long courseOfferingId,
    String subjectName,
    Long cohortId,
    String cohortName,
    double termHoursContributed,
    Long cohortSectionId,
    Long batchId,
    String cohortSectionLabel,
    String batchName,
    String sessionType
) {}
