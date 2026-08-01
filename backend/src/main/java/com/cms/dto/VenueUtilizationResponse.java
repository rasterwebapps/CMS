package com.cms.dto;

public record VenueUtilizationResponse(
    Long id,
    String name,
    Integer capacity,
    long occupiedSlots,
    int totalSlots,
    double utilizationPercent
) {}
