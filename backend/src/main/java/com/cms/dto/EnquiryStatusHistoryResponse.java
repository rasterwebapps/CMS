package com.cms.dto;

import java.time.Instant;

public record EnquiryStatusHistoryResponse(
    Long id,
    Long enquiryId,
    String fromStatus,
    String toStatus,
    String changedBy,
    Instant changedAt,
    String remarks
) {}
