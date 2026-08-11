package com.cms.dto;

import java.util.List;

/** One subject's placement budgets within a cohort-wide skeleton — {@link SkeletonBuilderResponse}
 *  carries one of these per {@code CourseOffering} the cohort has in the term, elective or not.
 *  {@code electiveGroupId}/{@code electiveGroupName} are non-null only for a grouped elective
 *  subject (see {@code CurriculumElectiveGroup}) — every subject sharing the same group id must
 *  be placed in the same day/period, enforced by {@code TimetableSkeletonService#checkElectiveGroupSlot}. */
public record SkeletonSubjectResponse(
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    List<SkeletonSubjectBudget> budgets,
    Long electiveGroupId,
    String electiveGroupName
) {}
