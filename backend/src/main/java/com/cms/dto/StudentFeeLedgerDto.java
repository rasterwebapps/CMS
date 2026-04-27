package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.cms.model.enums.DemandStatus;

public record StudentFeeLedgerDto(
    Long studentId,
    String studentName,
    List<LedgerEntry> entries
) {
    public record LedgerEntry(
        Long demandId,
        String termLabel,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        LocalDate dueDate,
        DemandStatus status,
        List<TermFeePaymentDto> payments
    ) {}
}
