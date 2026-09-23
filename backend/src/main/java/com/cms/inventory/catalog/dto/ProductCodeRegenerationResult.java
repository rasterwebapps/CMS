package com.cms.inventory.catalog.dto;

import java.util.List;

public record ProductCodeRegenerationResult(
    int totalChanged,
    List<ProductCodeChange> changes
) {}
