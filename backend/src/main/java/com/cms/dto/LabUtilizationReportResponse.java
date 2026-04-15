package com.cms.dto;

import java.util.Map;

public record LabUtilizationReportResponse(
    Long totalLabs,
    Long totalSchedules,
    Double averageSchedulesPerLab,
    Long totalEquipment,
    Map<String, Long> equipmentByStatus,
    Map<String, Long> labsByStatus
) {
    /** Backward-compatible constructor used in tests. */
    public LabUtilizationReportResponse(Long totalLabs, Long totalSchedules, Double averageSchedulesPerLab) {
        this(totalLabs, totalSchedules, averageSchedulesPerLab, 0L, Map.of(), Map.of());
    }
}
