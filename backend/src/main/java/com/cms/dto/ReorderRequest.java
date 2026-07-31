package com.cms.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/** Generic reorder payload: the full set of sibling ids, in the desired display order. Used for
 *  Zone-within-Floor and Room-within-Zone drag-to-reorder in the Campus Setup skyline — must
 *  contain exactly the ids currently belonging to the target parent, never a partial list or an
 *  id from a different parent. */
public record ReorderRequest(
    @NotEmpty(message = "orderedIds must not be empty")
    List<Long> orderedIds
) {}
