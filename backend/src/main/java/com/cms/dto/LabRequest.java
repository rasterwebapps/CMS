package com.cms.dto;

import com.cms.model.enums.LabStatus;
import com.cms.model.enums.LabType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LabRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotNull(message = "Lab type is required")
    LabType labType,

    @NotNull(message = "Department ID is required")
    Long departmentId,

    @Size(max = 255, message = "Building must not exceed 255 characters")
    String building,

    @Size(max = 50, message = "Room number must not exceed 50 characters")
    String roomNumber,

    @Min(value = 1, message = "Capacity must be positive")
    Integer capacity,

    @NotNull(message = "Status is required")
    LabStatus status
) {}
