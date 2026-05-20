package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CountryRequest(
    @NotBlank(message = "Country name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    String name,

    @NotBlank(message = "ISO code is required")
    @Size(min = 2, max = 3, message = "ISO code must be 2–3 characters (e.g. IN, US, GB)")
    String isoCode,

    Boolean isActive
) {}

