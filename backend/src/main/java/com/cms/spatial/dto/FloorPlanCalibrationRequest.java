package com.cms.spatial.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Two SVG-space points plus the known real-world distance between them. */
public record FloorPlanCalibrationRequest(
    @NotNull(message = "point1X is required")
    Double point1X,

    @NotNull(message = "point1Y is required")
    Double point1Y,

    @NotNull(message = "point2X is required")
    Double point2X,

    @NotNull(message = "point2Y is required")
    Double point2Y,

    @NotNull(message = "physicalLength is required")
    @Positive(message = "physicalLength must be positive")
    Double physicalLength
) {}
