package com.cms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record RoomRelocationRequest(
    @NotNull(message = "Date is required") LocalDate date,
    @NotNull(message = "Venue is required") Long venueId
) {}
