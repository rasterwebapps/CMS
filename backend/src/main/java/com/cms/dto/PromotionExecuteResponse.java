package com.cms.dto;

import java.util.List;

public record PromotionExecuteResponse(
    int promotedCount,
    int promotedWithArrearsCount,
    int detainedCount,
    int graduatedCount,
    int excludedCount,
    List<PromotionRejectedDecision> rejectedDecisions,
    Integer courseRegistrationsGenerated,
    Integer feeDemandsGenerated
) {}
