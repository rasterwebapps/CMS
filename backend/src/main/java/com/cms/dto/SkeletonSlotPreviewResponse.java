package com.cms.dto;

import com.cms.model.enums.DayOfWeek;

/** One (day, period) grid slot's live legality for dragging a specific already-placed skeleton
 *  cell there — powers Skeleton Builder's drag highlight so an admin sees which slots would
 *  actually accept the drop before releasing it, instead of finding out only after a rejected
 *  drop. {@code reason} is the first violation's human-readable message when {@code valid} is
 *  false (matching {@code TimetableSkeletonService#validateMoveTarget}'s own violation order),
 *  null when valid. Read-only and non-reserving — a slot reported valid here can still fail a
 *  moment later if another admin places something into it first; the real move endpoint remains
 *  the source of truth. */
public record SkeletonSlotPreviewResponse(
    DayOfWeek dayOfWeek,
    Long periodId,
    boolean valid,
    String reason
) {}
