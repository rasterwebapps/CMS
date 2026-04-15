package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.cms.model.enums.EnquirySource;
import com.cms.model.enums.EnquiryStatus;

public record EnquiryResponse(
    Long id,
    String name,
    String email,
    String phone,
    Long programId,
    String programName,
    LocalDate enquiryDate,
    EnquirySource source,
    EnquiryStatus status,
    Long agentId,
    String agentName,
    Long referralTypeId,
    String referralTypeName,
    BigDecimal referralGuidelineValue,
    String assignedTo,
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
