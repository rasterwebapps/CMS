package com.cms.inventory.catalog.dto;

import java.time.Instant;
import java.util.List;

public record ProductUomChainVersionResponse(
    Long id,
    Integer versionNo,
    Boolean isActive,
    String createdBy,
    Instant createdAt,
    List<ProductUomLevelResponse> levels
) {}
