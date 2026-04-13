package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.EquipmentCategory;
import com.cms.model.enums.EquipmentStatus;

public record EquipmentResponse(
    Long id,
    String name,
    String assetCode,
    String serialNumber,
    EquipmentCategory category,
    Long labId,
    String labName,
    String manufacturer,
    String model,
    EquipmentStatus status,
    LocalDate purchaseDate,
    BigDecimal purchasePrice,
    LocalDate warrantyExpiry,
    String location,
    String specifications,
    Instant createdAt,
    Instant updatedAt
) {}
