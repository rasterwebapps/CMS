package com.cms.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import com.cms.model.enums.DayOfWeek;

public record ClinicalShiftGroupDto(
    Long id,
    Long courseOfferingId,
    String subjectName,
    Long cohortSectionId,
    String cohortSectionLabel,
    Long termInstanceId,
    String label,
    DayOfWeek dayOfWeek,
    LocalTime clinicalStartTime,
    LocalTime clinicalEndTime,
    LocalTime busDepartTime,
    LocalTime busReturnTime,
    Boolean isActive,
    List<ClinicalShiftBatchLinkDto> batches,
    List<ClinicalShiftTheoryBlockDto> theoryBlocks,
    Instant createdAt,
    Instant updatedAt
) {}
