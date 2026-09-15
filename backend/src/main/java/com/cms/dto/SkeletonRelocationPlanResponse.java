package com.cms.dto;

import java.util.List;

import com.cms.model.enums.DayOfWeek;

/** Whether a session (single period or multi-period block) could go to the same-length window
 *  starting at {@code dayOfWeek}/{@code startPeriodId}, and what would move if it did. {@code kind}
 *  is {@code MOVE} when the window is empty and {@code SWAP} when the sessions already there would
 *  trade places into the block's old periods. {@code reason} explains an invalid window in the
 *  admin's terms; {@code moves} is filled for a valid one (the block itself first). */
public record SkeletonRelocationPlanResponse(
    DayOfWeek dayOfWeek,
    Long startPeriodId,
    List<Long> periodIds,
    String kind,
    boolean valid,
    String reason,
    List<SkeletonPlannedMove> moves
) {}
