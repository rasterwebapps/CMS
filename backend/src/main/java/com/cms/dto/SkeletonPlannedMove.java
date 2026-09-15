package com.cms.dto;

import java.util.List;

import com.cms.model.enums.ClassSessionType;
import com.cms.model.enums.DayOfWeek;

/** One session (a single period or a whole multi-period block, with every parallel batch that
 *  moves alongside it) that a Skeleton Builder relocation or Clinical duty-day change would move —
 *  the before → after rows the preview dialog shows before anything is applied. */
public record SkeletonPlannedMove(
    String subjectCode,
    ClassSessionType sessionType,
    /** Section or batch names the session belongs to, e.g. "Section 1" or "Batch A, Batch B". */
    String occupantLabel,
    DayOfWeek fromDay,
    List<Long> fromPeriodIds,
    DayOfWeek toDay,
    List<Long> toPeriodIds
) {}
