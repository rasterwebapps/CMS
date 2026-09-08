package com.cms.inventory.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record ProductAttributeValueRequest(
    @NotNull(message = "Attribute id is required") Long attributeId,
    String value
) {}
