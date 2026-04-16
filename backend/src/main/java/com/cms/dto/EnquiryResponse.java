package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.EnquiryStatus;

public record EnquiryResponse(
    Long id,
    String name,
    String email,
    String phone,
    Long programId,
    String programName,
    Long courseId,
    String courseName,
    LocalDate enquiryDate,
    Long referralTypeId,
    String referralTypeName,
    BigDecimal referralCommissionAmount,
    Boolean referralHasCommission,
    EnquiryStatus status,
    Long agentId,
    String agentName,
    String remarks,
    BigDecimal feeDiscussedAmount,
    BigDecimal feeGuidelineTotal,
    BigDecimal referralAdditionalAmount,
    BigDecimal finalCalculatedFee,
    String yearWiseFees,
    BigDecimal finalizedTotalFee,
    BigDecimal finalizedDiscountAmount,
    String finalizedDiscountReason,
    BigDecimal finalizedNetFee,
    String finalizedBy,
    Instant finalizedAt,
    Long convertedStudentId,
    Instant createdAt,
    Instant updatedAt
) {}
