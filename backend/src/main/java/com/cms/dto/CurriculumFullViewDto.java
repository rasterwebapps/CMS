package com.cms.dto;

import java.util.List;

import com.cms.model.enums.AssessmentPattern;

public record CurriculumFullViewDto(
    Long curriculumVersionId,
    String curriculumVersionName,
    Long programId,
    String programName,
    AssessmentPattern assessmentPattern,
    Integer totalSemesters,
    List<SemesterGroup> semesters
) {
    public record SemesterGroup(
        Integer semesterNumber,
        List<CurriculumSemesterCourseDto> courses
    ) {}
}
