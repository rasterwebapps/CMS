package com.cms.dto;

/** Outcome of replacing what a Theory cell teaches.
 *
 *  <p>{@code displaced} is the point of this being its own response type rather than a bare {@link
 *  SkeletonCellResponse}: replacing takes a slot away from the previous subject, so that subject is
 *  now delivering one session per week fewer than its curriculum requires — somewhere else in the
 *  term, invisibly. Reporting the resulting shortfall straight back means the admin is told
 *  immediately that it needs re-placing, instead of discovering it later from an hours card that
 *  quietly stopped adding up, or not at all.
 *
 *  <p>Null when the replacement displaced nothing that matters — the cell had no previous offering,
 *  or the previous subject is still meeting its requirement even without this slot (it was over its
 *  quota, or another session already covers it). */
public record SkeletonCellReplaceResponse(
    SkeletonCellResponse cell,
    DisplacedSubjectShortfall displaced
) {

    /** How far below its weekly curriculum requirement the displaced subject now sits, for this
     *  exact audience (section). {@code shortfallSessions} is always at least 1 when present —
     *  it is the number of sessions per week still to be placed elsewhere. */
    public record DisplacedSubjectShortfall(
        Long courseOfferingId,
        String subjectName,
        String subjectCode,
        Long cohortSectionId,
        String cohortSectionLabel,
        int requiredSessionsPerWeek,
        int placedSessionsPerWeek,
        int shortfallSessions
    ) {}
}
