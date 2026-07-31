package com.cms.dto;

import java.util.List;

public record ResourceGridRowResponse(
    Long resourceId,
    String resourceName,
    List<ResourceGridCellResponse> sessions
) {}
