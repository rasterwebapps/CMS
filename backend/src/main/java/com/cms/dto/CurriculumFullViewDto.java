package com.cms.dto;

import java.util.List;

public record CurriculumFullViewDto(
    Long curriculumVersionId,
    String curriculumVersionName,
    Long programId,
    String programName,
    Integer totalSemesters,
    List<SemesterGroup> semesters
) {
    public record SemesterGroup(
        Integer semesterNumber,
        List<CurriculumSemesterCourseDto> courses
    ) {}
}
