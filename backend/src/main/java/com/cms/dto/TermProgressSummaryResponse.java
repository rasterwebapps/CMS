package com.cms.dto;

import java.util.List;

public record TermProgressSummaryResponse(
    Long termInstanceId,
    List<SubjectProgressSummaryDto> subjects,
    double overallPercentComplete
) {}
