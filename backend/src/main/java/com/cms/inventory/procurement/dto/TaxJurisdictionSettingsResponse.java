package com.cms.inventory.procurement.dto;

import java.time.Instant;

public record TaxJurisdictionSettingsResponse(
    String homeState,
    Instant updatedAt,
    String updatedBy
) {}
