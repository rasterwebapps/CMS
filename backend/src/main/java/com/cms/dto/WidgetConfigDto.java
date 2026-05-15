package com.cms.dto;

/**
 * Represents one widget slot in a dashboard layout.
 * Returned by /permissions/my and /dashboard/config so the frontend
 * can render the correct widget at the correct grid position.
 *
 * colSpan / rowSpan map to the CSS grid: 1 = compact, 2 = half, 4 = full-width.
 */
public record WidgetConfigDto(
    String key,
    int    order,
    int    colSpan,
    int    rowSpan,
    String configJson
) {}
