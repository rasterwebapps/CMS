package com.cms.dto;

import java.time.Instant;
import java.util.List;

import com.cms.model.enums.CohortRoomAllocationStatus;
import com.cms.model.enums.PlanningBasis;

public record CohortRoomAllocationResponse(
    Long id,
    Long cohortId,
    String cohortLabel,
    Long termInstanceId,
    String termLabel,
    CohortRoomAllocationStatus status,
    PlanningBasis planningBasis,
    Integer plannedStrength,
    List<CohortSectionResponse> sections,
    String committedBy,
    Instant committedAt,
    String revertedBy,
    Instant revertedAt,
    List<AllocatedBatchResponse> batches
) {}
