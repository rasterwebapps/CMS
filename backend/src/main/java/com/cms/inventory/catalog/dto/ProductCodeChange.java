package com.cms.inventory.catalog.dto;

public record ProductCodeChange(
    Long productId,
    String productName,
    String categoryName,
    String oldCode,
    String newCode
) {}
