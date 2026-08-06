package com.cms.dto;

import java.time.LocalDate;

/** What RotationResolverService resolves for one rotation slot on one calendar date — which
 *  member/batch is actually in the room that day. */
public record RotationEffectiveResponse(
    Long rotationGroupId,
    Long classScheduleId,
    LocalDate date,
    Long rotationMemberId,
    String memberLabel,
    Long batchId,
    String batchName
) {}
