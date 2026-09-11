package com.cms.inventory.procurement.dto;

import java.time.Instant;

public record InventoryCurrencySettingsResponse(
    String baseCurrencyCode,
    Instant updatedAt,
    String updatedBy
) {}
