package com.cms.inventory.asset.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record AssetMaintenanceMarkPerformedRequest(
    LocalDate performedDate,
    @Size(max = 500) String notes
) {}
