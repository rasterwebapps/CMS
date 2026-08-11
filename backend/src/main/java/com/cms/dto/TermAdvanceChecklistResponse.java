package com.cms.dto;

import java.math.BigDecimal;
import java.util.List;

import com.cms.model.enums.TermInstanceStatus;

/** Live checklist data shown before an admin advances a term's status — system-verified where a
 *  reliable signal exists, so the confirmation the admin gives is informed rather than a blind
 *  "Continue?". Fields not relevant to {@code targetStatus} are empty/zero rather than null, so
 *  the frontend never has to null-check. */
public record TermAdvanceChecklistResponse(
    TermInstanceStatus targetStatus,

    /** OPEN only — active cohorts with no active curriculum version mapped, so opening the term
     *  would silently generate zero course offerings for them. */
    List<String> cohortsWithoutCurriculum,

    /** LOCKED only — demands still UNPAID/PARTIAL/WAIVED for this term. */
    int outstandingFeeDemandCount,
    BigDecimal outstandingFeeDemandAmount,

    /** LOCKED only — DRAFT ClassSchedule rows never approved before the term locks. */
    int draftTimetableSessionCount
) {}
