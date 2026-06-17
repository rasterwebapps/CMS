package com.cms.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CommissionExplorerResponse(
    Long enquiryId,
    String studentName,
    String admissionNumber,
    String enquiryStatus,
    String programName,
    String courseName,
    String enquiryDate,

    // Referral
    Long referralTypeId,
    String referralTypeName,
    String commissionSource,

    // Referral person
    Long agentId,
    String agentName,
    Long staffReferrerId,
    String staffReferrerName,
    Long referredFacultyId,
    String referredFacultyName,

    // Commission amounts
    BigDecimal commissionAmount,
    BigDecimal commissionPaidAmount,
    BigDecimal commissionOutstanding,
    String commissionPaymentStatus,

    // Payout history
    List<CommissionPayoutResponse> payouts,

    // OneBook tracking (null when OneBook is not involved)
    String oneBookReferenceId,
    String oneBookStatus,
    Instant oneBookTransmittedAt,
    String oneBookTxnId
) {}
