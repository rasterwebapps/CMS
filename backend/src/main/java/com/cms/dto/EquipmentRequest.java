package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cms.model.enums.EquipmentCategory;
import com.cms.model.enums.EquipmentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EquipmentRequest(
    @NotBlank(message = "Name is required")
    String name,

    String assetCode,

    String serialNumber,

    @NotNull(message = "Category is required")
    EquipmentCategory category,

    @NotNull(message = "Lab ID is required")
    Long labId,

    String manufacturer,

    String model,

    @NotNull(message = "Status is required")
    EquipmentStatus status,

    LocalDate purchaseDate,

    BigDecimal purchasePrice,

    LocalDate warrantyExpiry,

    String location,

    String specifications
) {}
