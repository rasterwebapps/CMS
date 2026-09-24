package com.cms.dto;

import java.util.List;

import com.cms.model.enums.RoomKind;

public record ResourceGridRowResponse(
    Long resourceId,
    String resourceName,
    List<ResourceGridCellResponse> sessions,
    /** Null for a FACULTY row (faculty ids are already their own single table, no collision risk).
     *  For a CLASSROOM-type row, identifies which of the three folded-together room tables
     *  resourceId actually belongs to -- see {@link RoomKind}'s own doc comment for why this
     *  matters. The frontend must round-trip this back on {@code openWeekView} so the "Full Week"
     *  drill-down can disambiguate resourceId correctly. */
    RoomKind roomKind
) {}
