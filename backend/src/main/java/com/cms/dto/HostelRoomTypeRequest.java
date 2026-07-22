package com.cms.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HostelRoomTypeRequest(

    @NotBlank(message = "Room type name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotBlank(message = "Room type code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    String code,

    @NotNull(message = "Sharing capacity is required")
    @Min(value = 1, message = "Sharing capacity must be at least 1")
    Integer sharingCapacity,

    Boolean isAc,

    @NotNull(message = "Fee amount per year is required")
    @DecimalMin(value = "0", inclusive = true, message = "Fee amount must not be negative")
    BigDecimal feeAmountPerYear,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    Boolean isActive
) {}
