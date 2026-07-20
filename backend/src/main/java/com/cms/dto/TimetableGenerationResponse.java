package com.cms.dto;

import java.util.List;

public record TimetableGenerationResponse(
    int generatedCount,
    List<String> unplaceable
) {}
