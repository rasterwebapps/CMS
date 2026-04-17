package com.cms.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentRequest(
    @NotBlank(message = "Name is required")
    String name,

    String phone,

    String email,

    String area,

    String locality,

    Integer allottedSeats,

    Boolean isActive
) {}
