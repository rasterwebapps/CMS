package com.cms.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceRequest(
    @NotNull @Valid List<PreferenceItem> preferences
) {
    public record PreferenceItem(
        @NotNull String categoryKey,
        @NotNull Boolean enabled,
        @NotNull String channel
    ) {}
}
