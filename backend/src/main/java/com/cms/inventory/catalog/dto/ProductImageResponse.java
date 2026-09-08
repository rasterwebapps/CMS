package com.cms.inventory.catalog.dto;

import java.time.Instant;

public record ProductImageResponse(
    Long id,
    Long productId,
    String originalFileName,
    String originalContentType,
    boolean isPrimary,
    Instant createdAt
) {}
