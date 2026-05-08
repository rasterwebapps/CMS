package com.cms.dto;

import java.util.List;

import com.cms.model.enums.AssessmentPattern;

public record CurriculumFullViewDto(
    Long curriculumVersionId,
    String curriculumVersionName,
    Long programId,
    String programName,
    AssessmentPattern assessmentPattern,
    Integer totalTerms,
    List<TermGroup> terms
) {
    public record TermGroup(
        Integer termNumber,
        List<CurriculumSemesterCourseDto> courses
    ) {}
}
