package com.cms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Create-only — a syllabus becomes immutable once created (see BR: syllabus versioning).
 *  Version is auto-assigned (next integer for the mapping), never client-supplied. */
public record SyllabusRequest(
    @NotNull(message = "Curriculum mapping (curriculum version + term + subject) is required")
    Long curriculumTermCourseId,

    @Size(max = 2000, message = "Objectives must not exceed 2000 characters")
    String objectives,

    @Size(max = 4000, message = "Content must not exceed 4000 characters")
    String content,

    @Size(max = 2000, message = "Text books must not exceed 2000 characters")
    String textBooks,

    @Size(max = 2000, message = "Reference books must not exceed 2000 characters")
    String referenceBooks,

    @Size(max = 2000, message = "Course outcomes must not exceed 2000 characters")
    String courseOutcomes,

    Boolean isActive
) {}
