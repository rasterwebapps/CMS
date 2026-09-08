package com.cms.inventory.asset.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AssetServiceContractResponse(
    Long id,
    Long assetId,
    String assetTag,
    Long supplierId,
    String supplierName,
    String contractNumber,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate renewalReminderDate,
    boolean expired,
    String coverageDetails,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
