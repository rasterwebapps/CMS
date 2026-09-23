package com.cms.exception;

import java.util.List;
import java.util.stream.Collectors;

import com.cms.dto.TimetableCoverageGap;

/** Thrown by {@code TimetableGenerationService#approve} when one or more cohorts still have
 *  curriculum-required Theory/Lab/Clinical hours that were never placed as real {@code
 *  ClassSchedule} rows for the term — the same "Total Unassigned" figure Skeleton Builder already
 *  shows per cohort, checked here at the one choke point that actually publishes a term. Unlike
 *  {@link TimetableConstraintViolationException} (a hard structural defect), this is overridable:
 *  the caller can resubmit with {@code overrideIncompleteCoverage=true} and a reason once an
 *  authorized reviewer accepts the gap. {@code gaps} is never empty when thrown. */
public class TimetableCoverageGapException extends RuntimeException {

    private final List<TimetableCoverageGap> gaps;

    public TimetableCoverageGapException(List<TimetableCoverageGap> gaps) {
        super(gaps.size() + " cohort/session-type combination(s) still have unscheduled curriculum hours: "
            + gaps.stream()
                .map(g -> g.cohortName() + " " + g.sessionType() + " (" + round(g.unassignedHours()) + "h short)")
                .collect(Collectors.joining("; ")));
        this.gaps = gaps;
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }

    public List<TimetableCoverageGap> getGaps() {
        return gaps;
    }
}
