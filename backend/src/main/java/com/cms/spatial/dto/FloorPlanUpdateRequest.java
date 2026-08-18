package com.cms.spatial.dto;

import com.cms.spatial.model.enums.OriginAnchor;
import com.cms.spatial.model.enums.UnitSystem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Metadata-only update — the file itself is replaced via the dedicated upload endpoint. */
public record FloorPlanUpdateRequest(
    @NotBlank(message = "entityType is required")
    String entityType,

    @NotNull(message = "entityId is required")
    Long entityId,

    @NotBlank(message = "name is required")
    String name,

    @NotNull(message = "unitSystem is required")
    UnitSystem unitSystem,

    @NotNull(message = "originAnchor is required")
    OriginAnchor originAnchor,

    @NotNull(message = "originX is required")
    Double originX,

    @NotNull(message = "originY is required")
    Double originY,

    Double viewboxWidth,
    Double viewboxHeight
) {}
