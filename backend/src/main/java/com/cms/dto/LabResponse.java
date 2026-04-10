package com.cms.dto;

import java.time.Instant;

import com.cms.model.enums.LabStatus;
import com.cms.model.enums.LabType;

public record LabResponse(
    Long id,
    String name,
    LabType labType,
    DepartmentResponse department,
    String building,
    String roomNumber,
    Integer capacity,
    LabStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
