package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HostelRoomTypeResponse(
    Long id,
    String name,
    String code,
    Integer sharingCapacity,
    Boolean isAc,
    BigDecimal feeAmountPerYear,
    String description,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
