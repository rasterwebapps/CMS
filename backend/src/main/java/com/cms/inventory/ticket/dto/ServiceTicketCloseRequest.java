package com.cms.inventory.ticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ServiceTicketCloseRequest(
    @Min(1) @Max(5) Integer feedbackRating
) {}
