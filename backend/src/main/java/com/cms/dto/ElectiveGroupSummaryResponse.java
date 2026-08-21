package com.cms.dto;

import com.cms.model.enums.ElectiveSelectionMode;

public record ElectiveGroupSummaryResponse(
    Long electiveGroupId,
    String electiveGroupName,
    ElectiveSelectionMode selectionMode,
    Integer termNumber,
    int eligibleCount,
    int assignedCount,
    boolean scheduled
) {}
