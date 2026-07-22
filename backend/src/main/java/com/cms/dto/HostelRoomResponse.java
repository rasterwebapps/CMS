package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HostelRoomResponse(
    Long id,
    Long roomId,
    String roomNumber,
    Long zoneId,
    String zoneName,
    Long roomTypeId,
    String roomTypeName,
    Integer sharingCapacity,
    Boolean isAc,
    BigDecimal feeAmountPerYear,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {}
