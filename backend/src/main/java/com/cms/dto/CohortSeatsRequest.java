package com.cms.dto;

public record CohortSeatsRequest(
    Integer managementSeats,
    Integer counsellingSeats
) {}
