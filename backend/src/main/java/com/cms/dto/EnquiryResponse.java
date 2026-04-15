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
    String assignedTo,
    String remarks,
    BigDecimal feeDiscussedAmount,
    Long convertedStudentId,
    Instant createdAt,
    Instant updatedAt
) {}
