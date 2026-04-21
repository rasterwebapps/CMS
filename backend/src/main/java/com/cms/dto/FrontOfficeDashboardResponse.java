package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Aggregated summary for the Front Office dashboard screen.
 */
public record FrontOfficeDashboardResponse(
    long todayEnquiryCount,
    long totalEnquiryCount,
    long pendingAdmissionsCount,
    BigDecimal feeCollectedToday,
    long conversionsThisWeek,
    double conversionRate,
    Map<String, Long> enquiryFunnel,
    List<FrontOfficeEnquiryItem> todaysEnquiries,
    List<String> pendingActionItems
) {}
