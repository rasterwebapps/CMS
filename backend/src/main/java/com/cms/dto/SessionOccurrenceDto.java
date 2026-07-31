package com.cms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SessionOccurrenceDto(
    Long id,
    Long classScheduleId,
    LocalDate occurrenceDate,
    List<UnitCoverageDto> unitCoverages,
    String remarks,
    Instant createdAt,
    Instant updatedAt
) {}
