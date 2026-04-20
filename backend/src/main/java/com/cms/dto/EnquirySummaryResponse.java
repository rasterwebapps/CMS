package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;

public record EnquirySummaryResponse(
    EnquiryResponse enquiry,
    BigDecimal totalAmountPaid,
    BigDecimal outstandingAmount,
    int documentCount,
    List<String> documentTypes
) {}
