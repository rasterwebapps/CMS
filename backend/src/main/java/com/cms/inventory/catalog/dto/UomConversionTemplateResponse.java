package com.cms.inventory.catalog.dto;

import java.time.Instant;
import java.util.List;

public record UomConversionTemplateResponse(
    Long id,
    String name,
    String description,
    Long baseUomId,
    String baseUomCode,
    String baseUomName,
    Boolean isActive,
    List<UomConversionTemplateLevelResponse> levels,
    Instant createdAt,
    Instant updatedAt
) {}
