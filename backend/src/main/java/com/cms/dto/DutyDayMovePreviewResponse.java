package com.cms.dto;

import java.util.List;

import com.cms.model.enums.DayOfWeek;

/** Whether a Clinical Shift group's duty could move to {@code dayOfWeek}, and which of that day's
 *  sessions inside the duty window would swap into the day the duty leaves. */
public record DutyDayMovePreviewResponse(
    DayOfWeek dayOfWeek,
    boolean valid,
    String reason,
    List<SkeletonPlannedMove> moves
) {}
