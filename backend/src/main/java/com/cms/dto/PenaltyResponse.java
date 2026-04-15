package com.cms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PenaltyResponse(
    Long studentId,
    String studentName,
    String rollNumber,
    BigDecimal totalPenalty,
    List<PenaltyDetail> penalties
) {
    public record PenaltyDetail(
        Long id,
        Long semesterFeeId,
        String semesterLabel,
        Integer yearNumber,
        BigDecimal dailyRate,
        LocalDate penaltyStartDate,
        LocalDate penaltyEndDate,
        long overdueDays,
        BigDecimal totalPenalty,
        Boolean isPaid
    ) {}
}
