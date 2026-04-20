package com.cms.dto;

import java.util.List;

public record DashboardTrendsResponse(
    List<DashboardTrendPoint> enrolmentTrend,
    List<DashboardTrendPoint> feeCollectionTrend
) {}
