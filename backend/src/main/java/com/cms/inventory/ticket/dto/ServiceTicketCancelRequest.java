package com.cms.inventory.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceTicketCancelRequest(
    @NotBlank @Size(max = 500) String reason
) {}
