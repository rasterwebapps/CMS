package com.cms.dto;

import java.util.List;

public record MyPermissionsResponse(
    String username,
    String roleName,
    String roleDisplayName,
    int hierarchyLevel,
    List<String> permissions,
    List<String> dashboardWidgets
) {}
