package com.cms.dto;

public record LabUtilizationReportResponse(
    Long totalLabs,
    Long totalSchedules,
    Double averageSchedulesPerLab
) {}
