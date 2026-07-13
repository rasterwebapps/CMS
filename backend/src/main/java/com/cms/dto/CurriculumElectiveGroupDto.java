package com.cms.dto;

import java.time.Instant;

public record CurriculumElectiveGroupDto(
    Long id,
    Long curriculumVersionId,
    Integer termNumber,
    String groupName,
    String groupCode,
    Instant createdAt,
    Instant updatedAt
) {}
