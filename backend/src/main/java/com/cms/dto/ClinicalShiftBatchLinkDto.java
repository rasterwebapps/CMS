package com.cms.dto;

public record ClinicalShiftBatchLinkDto(
    Long batchId,
    String batchName,
    Integer plannedSize,
    String venueLabel
) {}
