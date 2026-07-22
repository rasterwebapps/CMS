package com.cms.dto;

import jakarta.validation.constraints.NotNull;

import com.cms.model.enums.RoomAllocationStatus;

public record RoomAllocationStatusUpdateRequest(
    @NotNull(message = "Status is required")
    RoomAllocationStatus status
) {}
