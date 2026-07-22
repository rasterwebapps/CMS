package com.cms.dto;

import jakarta.validation.constraints.NotNull;

public record HostelRoomRequest(
    @NotNull(message = "Room type is required")
    Long roomTypeId,

    Boolean isActive
) {}
