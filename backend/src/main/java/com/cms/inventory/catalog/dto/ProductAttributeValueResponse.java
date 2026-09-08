package com.cms.inventory.catalog.dto;

public record ProductAttributeValueResponse(
    Long attributeId,
    String attributeName,
    String dataType,
    String value
) {}
