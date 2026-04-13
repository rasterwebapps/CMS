package com.cms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SyllabusRequest(
    @NotNull(message = "Course ID is required")
    Long courseId,

    @NotNull(message = "Version is required")
    @Positive(message = "Version must be positive")
    Integer version,

    @Positive(message = "Theory hours must be positive")
    Integer theoryHours,

    @Positive(message = "Lab hours must be positive")
    Integer labHours,

    @Positive(message = "Tutorial hours must be positive")
    Integer tutorialHours,

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
