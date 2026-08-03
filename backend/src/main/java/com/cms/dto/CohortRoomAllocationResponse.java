package com.cms.dto;

import java.time.Instant;
import java.util.List;

import com.cms.model.enums.CohortRoomAllocationStatus;

public record CohortRoomAllocationResponse(
    Long id,
    Long cohortId,
    String cohortLabel,
    Long termInstanceId,
    String termLabel,
    CohortRoomAllocationStatus status,
    Long theoryClassroomId,
    String theoryClassroomName,
    Integer theoryClassroomCapacity,
    String committedBy,
    Instant committedAt,
    String revertedBy,
    Instant revertedAt,
    List<AllocatedBatchResponse> batches
) {}
