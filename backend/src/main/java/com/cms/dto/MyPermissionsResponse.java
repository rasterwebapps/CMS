package com.cms.dto;

import java.util.List;

public record MyPermissionsResponse(
    String username,
    String fullName,
    String roleName,
    String roleDisplayName,
    int hierarchyLevel,
    List<String> permissions,
    List<WidgetConfigDto> dashboardWidgets
) {}
