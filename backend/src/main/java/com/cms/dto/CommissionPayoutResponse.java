package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CommissionPayoutResponse(
    Long id,
    BigDecimal amount,
    LocalDate payoutDate,
    String paymentMode,
    String transactionReference,
    String remarks,
    String paidBy,
    Instant createdAt
) {}
