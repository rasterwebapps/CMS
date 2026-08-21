package com.cms.dto;

import java.util.List;

/** {@code applicable=false} means this offering's cohort couldn't be uniquely resolved (shared
 *  across cohorts, or none currently enrolled) -- {@code reason} explains why, {@code sections} is
 *  empty. The frontend should also treat fewer than 2 sections as "nothing to show" even when
 *  {@code applicable} is true, since a single-section cohort has no split to assign. */
public record CourseOfferingSectionFacultyResponse(
    boolean applicable,
    String reason,
    List<SectionFacultyAssignment> sections
) {}
