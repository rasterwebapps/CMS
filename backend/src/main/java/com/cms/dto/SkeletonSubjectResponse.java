package com.cms.dto;

import java.util.List;

/** One subject's placement budgets within a cohort-wide skeleton — {@link SkeletonBuilderResponse}
 *  carries one of these per non-elective {@code CourseOffering} the cohort has in the term. */
public record SkeletonSubjectResponse(
    Long courseOfferingId,
    String subjectName,
    String subjectCode,
    List<SkeletonSubjectBudget> budgets
) {}
