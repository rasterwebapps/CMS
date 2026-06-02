package com.cms.dto;

public record SeatAvailabilityResponse(
    boolean available,
    long    filled,
    Integer total,
    boolean full,
    boolean closed,
    boolean overManagementQuota
) {}
