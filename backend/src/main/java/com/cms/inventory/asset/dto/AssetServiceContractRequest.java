package com.cms.inventory.asset.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssetServiceContractRequest(
    @NotNull Long assetId,
    @NotNull Long supplierId,
    @Size(max = 100) String contractNumber,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    LocalDate renewalReminderDate,
    @Size(max = 1000) String coverageDetails,
    Boolean isActive
) {}
