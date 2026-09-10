package com.cms.inventory.procurement.dto;

import java.time.Instant;

public record TaxTypeResponse(
    Long id,
    String name,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
