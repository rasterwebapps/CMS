package com.cms.dto;

public record NotificationPreferenceResponse(
    String categoryKey,
    Boolean enabled,
    String channel
) {}
