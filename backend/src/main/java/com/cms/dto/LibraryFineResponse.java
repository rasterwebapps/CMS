package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.cms.model.enums.FineStatus;

public record LibraryFineResponse(
    Long id,
    int overdueDays,
    BigDecimal finePerDay,
    BigDecimal totalFine,
    FineStatus status,
    String waivedBy,
    Instant collectedAt,
    String remarks
) {}
