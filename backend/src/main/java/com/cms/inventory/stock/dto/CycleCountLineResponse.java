package com.cms.inventory.stock.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * {@code systemQtySnapshot} and {@code varianceQty} are {@code null} while the parent count is
 * still DRAFT — the blind-count design deliberately keeps them out of every response the count-
 * entry screen reads until the count is submitted, rather than exposing a second DTO shape just
 * for that. See the 2026-09-08 "Cycle Count slice" decision-log entry.
 */
public record CycleCountLineResponse(
    Long id,
    Long productId,
    String productCode,
    String productName,
    String uomCode,
    BigDecimal systemQtySnapshot,
    BigDecimal countedQty,
    BigDecimal varianceQty,
    String status,
    String countedBy,
    Instant countedAt,
    String resolvedBy,
    Instant resolvedAt,
    String resolutionNotes,
    Long ledgerRefId,
    String notes
) {}
