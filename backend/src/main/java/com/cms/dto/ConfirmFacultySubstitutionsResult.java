package com.cms.dto;

/** Result of {@code CourseOfferingSectionFacultyService#confirmSubstitutions}. A genuine external
 *  conflict (a row's live faculty doesn't match what the run captured, and it wasn't this same
 *  batch's own earlier write) still rolls back the whole batch and throws instead of returning
 *  here. But when two tips in the same batch both claim the same {@code (cohortId,
 *  cohortSectionId)} row — because that section's sessions split across two different substitutes
 *  within one run — the first tip to reach that row wins it and the later tip's claim on that one
 *  row is skipped rather than aborting everything; {@code sectionsSkipped} counts those. */
public record ConfirmFacultySubstitutionsResult(
    int offeringsUpdated,
    int rowsReassigned,
    int sectionsSkipped
) {}
